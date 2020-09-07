package util.acl

import javax.inject.Inject
import io.suggest.n2.node.{MNodeTypes, MNodes}
import models.req.{MAdReq, MReq}
import play.api.mvc._
import io.suggest.es.model.EsModel
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 13:44
 * Description:
 * Абстрактная логика обработки запроса суперюзера на какое-либо действие с рекламной карточкой.
 */
final class IsSuMad @Inject()(
                               injector   : Injector,
                               esModel    : EsModel,
                               mNodes     : MNodes,
                               aclUtil    : AclUtil,
                               reqUtil    : ReqUtil,
                             ) {

  import esModel.api._

  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /**
    * @param adId id запрашиваемой рекламной карточки.
    */
  def apply(adId: String): ActionBuilder[MAdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdReq[A]) => Future[Result]): Future[Result] = {

        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {
          val madOptFut = mNodes
            .getByIdCache(adId)
            .withNodeType(MNodeTypes.Ad)

          madOptFut.flatMap {
            case Some(mad) =>
              val req1 = MAdReq(mad, request, user)
              block(req1)
            case None =>
              madNotFound(request)
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

      def madNotFound(request: Request[_]): Future[Result] =
        errorHandler.onClientError( request, Status.NOT_FOUND, s"ad not found: $adId" )

    }
  }

}
