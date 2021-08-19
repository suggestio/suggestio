package util.acl

import io.suggest.es.model.EsModel

import javax.inject.Inject
import io.suggest.n2.node.{MNodeTypes, MNodes}
import models.req.MAdReq
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 17:32
 * Description: ActionBuild для запроса действия над любой карточкой без проверки прав.
 */
final class GetAnyAd @Inject() (
                                 injector   : Injector,
                                 aclUtil    : AclUtil,
                                 reqUtil    : ReqUtil,
                               ) {

  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._


  /** Публичный доступ к указанной рекламной карточке.
    *
    * @param adId id рекламной карточки.
    */
  def apply(adId: String): ActionBuilder[MAdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdReq[A]) => Future[Result]): Future[Result] = {
        val madOptFut = mNodes
          .getByIdCache(adId)
          .withNodeType(MNodeTypes.Ad)

        val user = aclUtil.userFromRequest(request)

        madOptFut.flatMap {
          case Some(mad) =>
            val req1 = MAdReq(mad, request, user)
            block(req1)
          case None =>
            adNotFound(request)
        }
      }

      /** Что возвращать, если карточка не найдена. */
      def adNotFound(request: RequestHeader): Future[Result] =
        errorHandler.onClientError( request, Status.NOT_FOUND, s"Ad not found: $adId")

    }
  }

}
