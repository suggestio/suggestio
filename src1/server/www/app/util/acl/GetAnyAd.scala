package util.acl

import javax.inject.Inject
import io.suggest.model.n2.node.MNodeTypes
import models.mproj.ICommonDi
import models.req.MAdReq
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.Status

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 17:32
 * Description: ActionBuild для запроса действия над любой карточкой без проверки прав.
 */
class GetAnyAd @Inject() (
                           aclUtil    : AclUtil,
                           reqUtil    : ReqUtil,
                           mCommonDi  : ICommonDi
                         ) {

  import mCommonDi._


  /** Публичный доступ к указанной рекламной карточке.
    *
    * @param adId id рекламной карточки.
    */
  def apply(adId: String): ActionBuilder[MAdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdReq[A]) => Future[Result]): Future[Result] = {
        val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)

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
