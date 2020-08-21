package util.acl

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req._
import play.api.mvc._
import util.n2u.N2NodesUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:24
 * Description: Проверка прав на размещение рекламной карточки.
 */

/** Аддон для контроллеров для проверки права размещать рекламную карточку. */
class CanAdvAd @Inject()(
                          esModel                 : EsModel,
                          mNodes                  : MNodes,
                          aclUtil                 : AclUtil,
                          isNodeAdmin             : IsNodeAdmin,
                          n2NodeUtil              : N2NodesUtil,
                          reqUtil                 : ReqUtil,
                          mCommonDi               : ICommonDi
                        )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._


  /** Является ли указанный узел рекламодателем? */
  private def nodeCanProduceAdvs(mnode: MNode): Boolean = {
    mnode.common.isEnabled  &&  mnode.extras.adn.exists(_.isProducer)
  }

  def maybeAllowed[A](adId: String, req: IReq[A]): Future[Option[MAdProdReq[A]]] = {
    val madOptFut = mNodes
      .getByIdCache(adId)
      .withNodeType(MNodeTypes.Ad)

    madOptFut.flatMap {
      case Some(mad) =>
        maybeAllowed(mad, req)
      case None =>
        LOGGER.warn(s"maybeAllowed($adId): Ad not found, requested by u#${req.user}: $req")
        Future.successful(None)
    }
  }

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param mad Рекламная карточка.
   * @param req Реквест sio.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[models.req.MAdProdReq]]) если можно исполнять реквест.
   */
  def maybeAllowed[A](mad: MNode, req: IReq[A]): Future[Option[MAdProdReq[A]]] = {
    val prodIdOpt = n2NodeUtil.madProducerId(mad)
    // TODO Далее говнокод какой-то, переписать.
    def prodOptFut = mNodes.maybeGetByIdCached( prodIdOpt )
    def req2(mnode: MNode) = MAdProdReq(mad, mnode, req, req.user)
    if (req.user.isSuper) {
      for (prodOpt <- prodOptFut) yield {
        val resOpt = prodOpt
          .filter { nodeCanProduceAdvs }
          .map { req2 }
        if (resOpt.isEmpty)
          LOGGER.debug(s"maybeAllowed(${req.user.personIdOpt}, ${mad.id.get}): superuser, but ad producer node $prodIdOpt is not allowed to advertise.")
        resOpt
      }

    } else {
      req.user.personIdOpt.fold {
        LOGGER.trace(s"maybeAllowed(${mad.id.get}): anonymous access prohibited")
        val r = Option.empty[MAdProdReq[A]]
        Future.successful(r)

      } { personId =>
        for (prodOpt <- prodOptFut) yield {
          val resOpt = prodOpt
            .filter { mnode =>
              val isOwnedByMe = isNodeAdmin.isNodeAdminCheckStrict(mnode, req.user)
              isOwnedByMe  &&  nodeCanProduceAdvs(mnode)
            }
            .map { req2 }
          if (resOpt.isEmpty)
            LOGGER.debug(s"maybeAllowed($personId, ${mad.id.get}): User is not node $prodIdOpt admin or node is not a producer.")
          resOpt
        }
      }
    }
  }


  /** Сборка ActionBuilder'а, проверяющего права на размещение карточки.
    *
    * @param adId id размещаемой рекламной карточки.
    */
  def apply(adId: String, userInits1: MUserInit*): ActionBuilder[MAdProdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdProdReq] {

      def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
        val madFut = mNodes
          .getByIdCache(adId)
          .withNodeType(MNodeTypes.Ad)

        val user = aclUtil.userFromRequest(request)

        // Оптимистично запустить сбор запрошенных данных MSioUser.
        MUserInits.initUser(user, userInits1)

        // Продолжить дальше работу асинхронно...
        val reqBlank = MReq(request, user)
        madFut.flatMap {
          // Карточка найден, проверить доступ...
          case Some(mad) =>
            maybeAllowed(mad, reqBlank).flatMap {
              case Some(req1) =>
                block(req1)
              case None =>
                LOGGER.debug(s"invokeBlock(): maybeAllowed(${user.personIdOpt.orNull}, mad=${mad.id.get}) -> false.")
                isNodeAdmin.onUnauthNode(reqBlank)
            }

          // Нет запрашиваем карточки, отработать и этот вариант.
          case None =>
            LOGGER.debug("invokeBlock(): MAd not found: " + adId)
            isNodeAdmin.onUnauthNode(reqBlank)
        }
      }
    }
  }

}
