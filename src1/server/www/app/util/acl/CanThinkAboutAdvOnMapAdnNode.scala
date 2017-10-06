package util.acl

import javax.inject.Inject

import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MAdProdRcvrReq, MReq}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import util.adv.geo.AdvGeoRcvrsUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 15:31
  * Description: ACL для проверок возможности размещения
  */
class CanThinkAboutAdvOnMapAdnNode @Inject() (
                                               aclUtil                : AclUtil,
                                               mNodes                 : MNodes,
                                               canAdvAd               : CanAdvAd,
                                               advGeoRcvrsUtil        : AdvGeoRcvrsUtil,
                                               isNodeAdmin            : IsNodeAdmin,
                                               isAuth                 : IsAuth,
                                               reqUtil                : ReqUtil,
                                               mCommonDi              : ICommonDi
                                             )
  extends MacroLogsImpl
{

  import mCommonDi._


  /** Собрать ACL ActionBuilder.
    * Нужно проверить права доступа к рекламной карточке (CanAdvertiseAd).
    * А также проверить, можно ли размещаться на указанный узел через карту.
    *
    * @param adId id рекламной карточки, которую юзер хочет разместить на узле.
    * @param nodeId id узла, на который юзер хочет что-то размещать.
    */
  def apply(adId: String, nodeId: String): ActionBuilder[MAdProdRcvrReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdProdRcvrReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdProdRcvrReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        user.personIdOpt.fold( isAuth.onUnauth(request) ) { _ =>
          // Юзер залогинен. Сразу же собираем все параллельные задачи...
          val madOptFut   = mNodesCache.getByIdType(adId,   MNodeTypes.Ad)

          // Ищем целевой узел, проверяя права размещения на узле прямо в рамках ES-запроса:
          val nodeOptFut = mNodes.dynSearchOne(
            advGeoRcvrsUtil.onMapRcvrsSearch(
              limit1      = 1,
              onlyWithIds = nodeId :: Nil
            )
          )

          // Подготовить синхронные данные:
          val reqBlank = MReq(request, user)
          lazy val logPrefix = s"${getClass.getSimpleName}[${reqBlank.remoteClientAddress}]#${user.personIdOpt.orNull}:"

          // Когда придёт ответ от БД по запрошенной карточке...
          madOptFut.flatMap {
            case Some(mad) =>
              canAdvAd.maybeAllowed(mad, reqBlank).flatMap {
                // Есть карточка, проверка прав на неё ок.
                case Some(req1) =>
                  nodeOptFut.flatMap {
                    // Всё ок и с узлом. Запускаем экшен.
                    case Some(mnode) =>
                      val req2 = MAdProdRcvrReq(
                        mad       = mad,
                        producer  = req1.producer,
                        mnode     = mnode,
                        request   = request,
                        user      = user
                      )
                      block(req2)

                    // Нет узла или нельзя на него размещать. Логгирование уже выполнено внутри nodeCheckedOptFut.
                    case None =>
                      isNodeAdmin.onUnauthNode(reqBlank)
                  }

                // Нет доступа к карточке. Обычно, когда сессия истекла.
                case None =>
                  LOGGER.debug(s"$logPrefix: maybeAllowed(mad#$adId) -> false.")
                  isNodeAdmin.onUnauthNode(reqBlank)
              }

            // Нет карточки такой вообще.
            case None =>
              LOGGER.debug(s"$logPrefix: MAd not found: $adId")
              isNodeAdmin.onUnauthNode(reqBlank)
          }
        }
      }

    }
  }

}
