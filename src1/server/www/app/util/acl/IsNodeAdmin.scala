package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mproj.ICommonDi
import models.req._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter

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
class IsNodeAdmin @Inject()(
                             aclUtil          : AclUtil,
                             isAuth           : IsAuth,
                             mCommonDi        : ICommonDi
                           )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauthNode(req: IReqHdr): Future[Result] = {
    if (req.user.isAuth) {
      Results.Forbidden("403 Forbidden")
    } else {
      isAuth.onUnauth(req)
    }
  }

  def checkAdnNodeCredsFut(nodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Either[Option[MNode], MNode]] = {
    for (nodeOpt <- nodeOptFut) yield {
      checkAdnNodeCreds(nodeOpt, adnId, user)
    }
  }

  def checkAdnNodeCreds(adnNodeOpt: Option[MNode], adnId: String, user: ISioUser): Either[Option[MNode], MNode] = {
    adnNodeOpt.fold [Either[Option[MNode], MNode]] {
      LOGGER.warn(s"checkAdnNodeCreds(): Node[$adnId] does not exist!")
      Left(None)
    } { adnNode =>
      val isAllowed = isAdnNodeAdminCheck(adnNode, user)
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

  def checkAdnNodeCredsOpt(nodeOptFut: Future[Option[MNode]], nodeId: String, user: ISioUser): Future[Option[MNode]] = {
    checkAdnNodeCredsFut(nodeOptFut, nodeId, user).map {
      case Right(mnode) =>
        Some(mnode)
      case other =>
        LOGGER.trace(s"checkAdnNodeCredsOpt($nodeId): u#$user => $other ")
        None
    }
  }

  def isAdnNodeAdmin(adnId: String, user: ISioUser): Future[Option[MNode]] = {
    val fut = mNodesCache.getById(adnId)
    checkAdnNodeCredsOpt(fut, adnId, user)
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

    lazy val logPrefix = s"isNodeChainAdmin($nodeKey, $user):"

    def __fold(
                ownersAcc  : Set[String]                  = user.personIdOpt.toSet,
                accRev     : List[MNode]                  = Nil,
                rest       : List[Future[Option[MNode]]]  = nodeKey.map { mNodesCache.getById },
                level      : Int                          = 1
              ): Future[Option[List[MNode]]] = {

      rest.headOption.fold [Future[Option[List[MNode]]]] {
        LOGGER.trace(s"$logPrefix Done ok. total levels = $level")
        Some( accRev.reverse )

      } { nodeOptFut =>
        nodeOptFut.flatMap {
          case Some(mnode) =>
            if ( user.isSuper || isAdnNodeAdminCheckStrict(mnode, ownersAcc) ) {
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
  def isAdnNodeAdminCheck(mnode: MNode, user: ISioUser): Boolean = {
    user.isSuper ||
      isAdnNodeAdminCheckStrict(mnode, user)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(mnode: MNode, user: ISioUser): Boolean = {
    isAdnNodeAdminCheckStrict(mnode, user.personIdOpt.toSet)
  }
  def isAdnNodeAdminCheckStrict(mnode: MNode, personIds: Set[String]): Boolean = {
    personIds.nonEmpty && {
      // Проверка admin-доступа к v2: проверять OwnedBy
      val allowedOwn = mnode.edges
         .withPredicateIterIds( MPredicates.OwnedBy )
        .exists( personIds.contains )

      allowedOwn || mnode.id.exists( personIds.contains )
    }
  }


  /**
    * Собрать ACL ActionBuilder, проверяющий права admin-доступа к узлу.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String, userInits1: MUserInit*): ActionBuilder[MNodeReq] = {
    new SioActionBuilderImpl[MNodeReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        val isAllowedFut = isAdnNodeAdmin(nodeId, user)

        if (user.personIdOpt.nonEmpty)
          maybeInitUser(user)

        isAllowedFut.flatMap {
          case Some(mnode) =>
            val req1 = MNodeReq(mnode, request, user)
            block(req1)

          case None =>
            LOGGER.debug(s"User#${user.personIdOpt.orNull} has NO admin access to node $nodeId")
            val req1 = MReq(request, user)
            onUnauthNode(req1)
        }
      }

    }
  }



  /**
    * Собрать ACL ActionBuilder, проверяющий права admin-доступа к узлу.
    * @param nodeKey Цепочка id'шников запрашиваемого узла.
    */
  def apply(nodeKey: RcvrKey, userInits1: MUserInit*): ActionBuilder[MNodesChainReq] = {
    new SioActionBuilderImpl[MNodesChainReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MNodesChainReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        val isAllowedFut = isNodeChainAdmin(nodeKey, user)

        if (user.personIdOpt.nonEmpty)
          maybeInitUser(user)

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
