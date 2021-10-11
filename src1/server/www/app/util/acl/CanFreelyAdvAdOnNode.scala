package util.acl

import javax.inject.Inject
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.err.HttpResultingException
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
        lazy val logPrefix = s"($adId, ${nodeKey.mkString("/")}):"

        val nodesChainOptFut = isNodeAdmin
          .isNodeChainAdmin(nodeKey, user)
          .filter(_.nonEmpty)
          .recoverWith {
            // For single-length paths use legacy path-less detection: view as node id only.
            // For example, LkNodes form => visible radio-beacons list => own radio-beacon => isAdv flag changed.
            case ex: Throwable if (nodeKey.lengthIs == 1) =>
              if (!ex.isInstanceOf[NoSuchElementException])
                LOGGER.warn( s"$logPrefix Recovering unexpected exception for nodesChain $nodeKey", ex )

              isNodeAdmin.isNodeAdminUpFrom(
                nodeId = nodeKey.head,
                user   = user,
              )
          }
          .recover { case ex: Throwable =>
            if (!ex.isInstanceOf[NoSuchElementException])
              LOGGER.warn( s"$logPrefix Unrecoverable exception for nodeChain ${nodeKey}", ex )
            None
          }

        (for {
          madProdReqOpt <- madReqOptFut
          madProdReq = madProdReqOpt getOrElse {
            // No access to ad-card for current user.
            LOGGER.warn(s"$logPrefix User have no access for ad#$adId.")
            throw new HttpResultingException(
              httpErrorHandler.onClientError( request, Status.FORBIDDEN, s"No access for ad#$adId" ),
            )
          }

          _ = MUserInits.initUser(user, userInits1)
          nodesChainOpt <- nodesChainOptFut
          nodesChain = nodesChainOpt getOrElse {
            // No access to pointed node. In normal flow, this just should not happen.
            LOGGER.warn(s"$logPrefix User#$user have adv access to ad#$adId, but nodes chain access validation failed.")
            throw new HttpResultingException(
              httpErrorHandler.onClientError( request, Status.FORBIDDEN, s"Failed in ${RcvrKey.rcvrKey2urlPath(nodeKey)}" )
            )
          }

          // Successfully verified node chain.
          mreq2 = MAdProdNodesChainReq(
            mad         = madProdReq.mad,
            producer    = madProdReq.producer,
            nodesChain  = nodesChain,
            request     = ireq,
            user        = user
          )
          result <- block( mreq2 )

        } yield {
          result
        })
          .recoverWith {
            case ex: HttpResultingException => ex.httpResFut
          }
      }

    }
  }

}
