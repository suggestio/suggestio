package util.acl

import javax.inject.Inject
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.util.logs.MacroLogsImpl
import models.req.{MAdProdNodesChainReq, MUserInit, MUserInits}
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 14:27
  * Description: ACL проверки возможности бесплатного управления прямым размещением карточки на каком-то либо узле.
  * Поддерживается задание id целевого узела задаётся по rcvrKey.
  */
final class CanFreelyAdvAdOnNode @Inject() (
                                             injector                 : Injector,
                                           )
  extends MacroLogsImpl
{

  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val canAdvAd = injector.instanceOf[CanAdvAd]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Выполнение проверки полного доступа к карточке и к цепочки узлов.
    *
    * @param adId id запрашиваемой рекламной карточки.
    * @param nodeKey Ключ узла в виде списка-цепочки узлов.
    * @param userInits1 Инициализация user-модели, если требуется.
    */
  def apply(adId: String, nodeKey: RcvrKey, userInits1: MUserInit*): ActionBuilder[MAdProdNodesChainReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdProdNodesChainReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdProdNodesChainReq[A]) => Future[Result]): Future[Result] = {
        val ireq = aclUtil.reqFromRequest(request)

        val madReqOptFut = canAdvAd.maybeAllowed(adId, ireq)
        val user = ireq.user
        val nodesChainOptFut = isNodeAdmin.isNodeChainAdmin(nodeKey, user)

        MUserInits.initUser(user, userInits1)

        def logPrefix = s"apply($adId, ${nodeKey.mkString("/")}):"

        madReqOptFut.flatMap {
          // Проверка доступа к карточке выполнена успешно.
          case Some(madProdReq) =>
            // Теперь проверить узлы, заданные в nodeKey:
            nodesChainOptFut.flatMap {
              // Успешно проверена вся цепочка узлов. Можно запускать код экшена.
              case Some(nodesChain) =>
                val mreq2 = MAdProdNodesChainReq(
                  mad         = madProdReq.mad,
                  producer    = madProdReq.producer,
                  nodesChain  = nodesChain,
                  request     = ireq,
                  user        = user
                )
                block(mreq2)

              // Был доступ к карточке, но нет доступа на указанный узел. В норме этой ситуации не должно возникать.
              case None =>
                LOGGER.warn(s"$logPrefix User#$user have adv access to ad#$adId, but nodes chain access validation failed.")
                httpErrorHandler.onClientError( request, Status.FORBIDDEN, s"Failed in ${RcvrKey.rcvrKey2urlPath(nodeKey)}" )
            }

          // Проверка прав на карточку рекламную не пройдена.
          case None =>
            LOGGER.warn(s"$logPrefix User have no access for ad#$adId.")
            httpErrorHandler.onClientError( request, Status.FORBIDDEN, s"No access for ad#$adId" )
        }

      }
    }
  }

}
