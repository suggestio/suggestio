package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import play.api.http.Status

import scala.concurrent.Future
import play.api.mvc._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */

/** Аддон для контроллеров для проверки admin-прав доступа к узлу. */
@Singleton
final class IsNodeAdmin @Inject()(
                                   mCommonDi        : ICommonDi
                                 )
  extends MacroLogsImpl
{

  import mCommonDi.current.injector
  import mCommonDi.{ec, errorHandler}

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val dab = injector.instanceOf[DefaultActionBuilder]

  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauthNode(req: IReqHdr): Future[Result] = {
    if (req.user.isAuth) {
      errorHandler.onClientError( req, Status.FORBIDDEN )
    } else {
      isAuth.onUnauth(req)
    }
  }

  def checkNodeCredsFut(nodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Either[Option[MNode], MNode]] = {
    for (nodeOpt <- nodeOptFut) yield {
      isUserAdminOfNode(nodeOpt, adnId, user)
    }
  }

  def isUserAdminOfNode(adnNodeOpt: Option[MNode], adnId: String, user: ISioUser): Either[Option[MNode], MNode] = {
    adnNodeOpt.fold [Either[Option[MNode], MNode]] {
      LOGGER.warn(s"checkAdnNodeCreds(): Node[$adnId] does not exist!")
      Left(None)
    } { adnNode =>
      val isAllowed = isNodeAdminCheck(adnNode, user)
      if (isAllowed) {
        Right(adnNode)
      } else {
        for (personId <- user.personIdOpt) {
          LOGGER.warn(s"checkAdnNodeCreds(): User $personId not allowed to access to node $adnId")
        }
        Left(adnNodeOpt)
      }
    }
  }

  def checkNodeCredsOpt(nodeOptFut: Future[Option[MNode]], nodeId: String, user: ISioUser): Future[Option[MNode]] = {
    checkNodeCredsFut(nodeOptFut, nodeId, user).map {
      case Right(mnode) =>
        Some(mnode)
      case other =>
        LOGGER.trace(s"checkAdnNodeCredsOpt($nodeId): u#$user => $other ")
        None
    }
  }

  def isAdnNodeAdmin(adnId: String, user: ISioUser): Future[Option[MNode]] = {
    import esModel.api._

    val fut = mNodes.getByIdCache(adnId)
    checkNodeCredsOpt(fut, adnId, user)
  }


  /** Проверка прав на узел путём поднятия вверх по цепочке узлов, которая выстраивается на лету.
    * Удобна для проверки доступа к узлам, которые известны только по id.
    * Появилась для проверки доступа юзеров на управление размещением на ресиверах.
    *
    * @param nodeId id проверяемого узла.
    * @param user Данные пользователя.
    * @param maxLevels Макс.кол-во итераций поднятия наверх.
    * @return Фьючерс с цепочкой вычисленных узлов от юзера до узла.
    *         Если SU-юзер, то цепочка всегжа будет иметь длину 1.
    */
  def isNodeAdminUpFrom(nodeId: String, user: ISioUser, maxLevels: Int = 3): Future[Option[List[MNode]]] = {
    lazy val logPrefix = s"isNodeAdminUpFrom(u#${user.personIdOpt.orNull} -> node#$nodeId)#${System.currentTimeMillis()}:"

    user.personIdOpt.fold {
      LOGGER.debug(s"$logPrefix Anonymous user prohibited")
      Future.successful( Option.empty[List[MNode]] )

    } { personId =>
      lazy val adnTreeNTypes = MNodeTypes.adnTreeMemberTypes
      val userIdSet = Set.empty + personId

      def _isNodeAdminAnyOfOrUpTo(currNodeId: String, counter: Int, accRev: List[MNode]): Future[Option[List[MNode]]] = {
        lazy val logPrefix2 = s"$logPrefix[$counter #$currNodeId]:"

        if (counter >= maxLevels) {
          // Гулять уже больше некуда.
          LOGGER.warn(s"$logPrefix2 Too many levels deep for checking, limit=$maxLevels reached, giving up.")
          Future.successful(None)

        } else {
          import esModel.api._

          for {
            // Получить на руки узел, который проверяется:
            mnodeOpt <- mNodes.getByIdCache(currNodeId)
            mnode = mnodeOpt.get

            // Есть ли доступ у юзера?
            res <- {
              lazy val ownerEdges = mnode.edges
                .withPredicateIter( MPredicates.OwnedBy )
                .to( LazyList )

              val userIsSuper = user.isSuper
              if ( userIsSuper || isNodeAdminCheckStrict(mnode, userIdSet) ) {
                // Всё ок, цепочка узлов построена.
                LOGGER.trace(s"$logPrefix2 User SU?$userIsSuper, allowed")
                Future.successful( Some(mnode :: accRev) )

              } else if (
                (adnTreeNTypes contains mnode.common.ntype) &&
                ownerEdges.exists(_.nodeIds.nonEmpty)
              ) {
                // Пока не понятно, есть ли доступ, но узел подразумевает, что доступ может быть с родительских узлов:
                val parentNodeIds = ownerEdges
                  .iterator
                  .flatMap(_.nodeIds)
                  .toSet
                LOGGER.trace( s"$logPrefix2 Found ${parentNodeIds.size} parent nodes: ${parentNodeIds.mkString(", ")}" )
                val nextCounter = counter + 1
                val nextAccRev = mnode :: accRev

                // Переход на проверку родитльских узлов
                for {
                  // Маппинг: Запустить рекурсивную проверку родительских узлов:
                  nodesAnalyzed <- Future.traverse(parentNodeIds) { parentNodeId =>
                    LOGGER.trace(s"$logPrefix2 Will check $parentNodeId...")
                    _isNodeAdminAnyOfOrUpTo(parentNodeId, nextCounter, nextAccRev)
                  }
                } yield {
                  // Редукция: найти в результатах первый успешный родительский узел (первый Some-результат).
                  nodesAnalyzed
                    .find(_.nonEmpty)
                    .flatten
                }

              } else {
                // Тип узла не подразумевает возможность иметь над-узел с доступом юзера. Значит, надо окончить обход этой ветви.
                LOGGER.trace(s"$logPrefix Node is NOT owned by user, and node type#${mnode.common.ntype} does not permits checking for upper levels.")
                Future.successful( None )
              }
            }

          } yield {
            LOGGER.trace(s"$logPrefix2 nodes chain => ${res.map(_.iterator.flatMap(_.id).mkString("/"))}")
            res
          }
        }
      }

      // Запустить цикл проверки, гуляя вверх от текущего узла.
      _isNodeAdminAnyOfOrUpTo(
        currNodeId  = nodeId,
        counter     = 0,
        accRev      = Nil
      )
    }
  }


  /** Rcvr-key может быть использован вместо id узла.
    *
    * @param nodeKey Ключ узла: цепочка из id'шников узлов.
    * @param user Данные по текущему юзера.
    * @return Фьючерс с опциональным результатом.
    *         None, если облом проверки прав (см. логи).
    *         Some + список узлов, где порядок строго соответствует nodeKey.
    */
  def isNodeChainAdmin(nodeKey: RcvrKey, user: ISioUser): Future[Option[List[MNode]]] = {
    import esModel.api._

    lazy val logPrefix = s"isNodeChainAdmin($nodeKey, $user):"

    def __fold(
                ownersAcc  : Set[String]                  = user.personIdOpt.toSet,
                accRev     : List[MNode]                  = Nil,
                rest       : List[Future[Option[MNode]]]  = nodeKey.map { mNodes.getByIdCache },
                level      : Int                          = 1
              ): Future[Option[List[MNode]]] = {

      rest.headOption.fold [Future[Option[List[MNode]]]] {
        LOGGER.trace(s"$logPrefix Done ok. total levels = $level")
        Some( accRev.reverse )

      } { nodeOptFut =>
        nodeOptFut.flatMap {
          case Some(mnode) =>
            if ( user.isSuper || isNodeAdminCheckStrict(mnode, ownersAcc) ) {
              LOGGER.trace(s"$logPrefix Ok for node#${mnode.idOrNull}, owners = ${ownersAcc.mkString(", ")}.")
              // Есть доступ на админство. Продолжаем обход.
              __fold(
                ownersAcc = ownersAcc ++ mnode.id,
                accRev    = mnode :: accRev,
                rest      = rest.tail,
                level     = level + 1
              )
            } else {
              LOGGER.warn(s"$logPrefix No access for node#${mnode.idOrNull} on level#$level from owners = ${ownersAcc.mkString(", ")}")
              None
            }

          case None =>
            LOGGER.warn(s"$logPrefix Node not found on level $level.")
            None
        }
      }
    }

    // Запустить поиск всех узлов, но последовательно все пройти слева направо.
    __fold()
  }


  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isNodeAdminCheck(mnode: MNode, user: ISioUser): Boolean = {
    user.isSuper ||
      isNodeAdminCheckStrict(mnode, user)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isNodeAdminCheckStrict(mnode: MNode, user: ISioUser): Boolean =
    isNodeAdminCheckStrict(mnode, user.personIdOpt.toSet)
  def isNodeAdminCheckStrict(mnode: MNode, personIds: Set[String]): Boolean = {
    personIds.nonEmpty && {
      // Проверка admin-доступа к v2: проверять OwnedBy
      val allowedOwn = mnode.edges
        .withPredicateIterIds( MPredicates.OwnedBy )
        .exists( personIds.contains )

      allowedOwn || mnode.id.exists( personIds.contains )
    }
  }


  private def _applyId[A](nodeId: String, userInits: Seq[MUserInit], request: Request[A])(f: MNodeReq[A] => Future[Result]): Future[Result] = {
    val user = aclUtil.userFromRequest(request)

    val isAllowedFut = isAdnNodeAdmin(nodeId, user)

    if (user.personIdOpt.nonEmpty)
      MUserInits.initUser(user, userInits)

    isAllowedFut.flatMap {
      case Some(mnode) =>
        val req1 = MNodeReq(mnode, request, user)
        f(req1)

      case None =>
        LOGGER.debug(s"User#${user.personIdOpt.orNull} has NO admin access to node $nodeId")
        val req1 = MReq(request, user)
        onUnauthNode(req1)
    }
  }


  /**
    * Собрать ACL ActionBuilder, проверяющий права admin-доступа к узлу.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String, userInits: MUserInit*): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        _applyId(nodeId, userInits, request)(block)
      }

    }
  }

  def A[A](nodeId: String, userInits: MUserInit*)(action: Action[A]): Action[A] = {
    dab.async(action.parser) { request =>
      _applyId(nodeId, userInits, request)(action.apply)
    }
  }


  /**
    * Собрать ACL ActionBuilder, проверяющий права admin-доступа к узлу.
    * @param nodeKey Цепочка id'шников запрашиваемого узла.
    */
  def apply(nodeKey: RcvrKey, userInits1: MUserInit*): ActionBuilder[MNodesChainReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodesChainReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodesChainReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        val isAllowedFut = isNodeChainAdmin(nodeKey, user)

        if (user.personIdOpt.nonEmpty)
          MUserInits.initUser(user, userInits1)

        isAllowedFut.flatMap {
          case Some(mnodes) =>
            val req1 = MNodesChainReq(mnodes, request, user)
            block(req1)

          case None =>
            LOGGER.debug(s"User#${user.personIdOpt.orNull} has NO admin access to one of nodes in chain $nodeKey")
            val req1 = MReq(request, user)
            onUnauthNode(req1)
        }
      }

    }
  }

}

/** Интерфейс для поля c DI-инстансом [[IsNodeAdmin]]. */
trait IIsNodeAdmin {
  val isNodeAdmin: IsNodeAdmin
}
