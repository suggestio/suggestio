package util.acl

import com.google.inject.Inject
import io.suggest.model.n2.node.MNodeTypes
import models.mproj.ICommonDi
import models.req.MAdReq
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 17:32
 * Description: ActionBuild для запроса действия над любой карточкой без проверки прав.
 */
class GetAnyAd @Inject() (mCommonDi: ICommonDi) extends SioActionBuilderOuter {

  import mCommonDi._


  /** Публичный доступ к указанной рекламной карточке.
    *
    * @param adId id рекламной карточки.
    */
  def apply(adId: String): ActionBuilder[MAdReq] = {
    new SioActionBuilderImpl[MAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdReq[A]) => Future[Result]): Future[Result] = {
        val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)

        val personIdOpt = sessionUtil.getPersonId(request)
        val user = mSioUsers(personIdOpt)

        madOptFut.flatMap {
          case Some(mad) =>
            val req1 = MAdReq(mad, request, user)
            block(req1)
          case None =>
            adNotFound(request)
        }
      }

      /** Что возвращать, если карточка не найдена. */
      def adNotFound(request: Request[_]): Future[Result] = {
        Results.NotFound("Ad not found: " + adId)
      }

    }
  }

}
