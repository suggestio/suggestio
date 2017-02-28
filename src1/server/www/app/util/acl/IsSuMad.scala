package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.model.n2.node.MNodeTypes
import models.mproj.ICommonDi
import models.req.{MAdReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 13:44
 * Description:
 * Абстрактная логика обработки запроса суперюзера на какое-либо действие с рекламной карточкой.
 */
@Singleton
class IsSuMad @Inject()(
                         isSu       : IsSu,
                         mCommonDi  : ICommonDi
                       )
  extends SioActionBuilderOuter
{

  import mCommonDi._


  /**
    * @param adId id запрашиваемой рекламной карточки.
    */
  def apply(adId: String): ActionBuilder[MAdReq] = {
    new SioActionBuilderImpl[MAdReq] {

      override def invokeBlock[A](request: Request[A], block: (MAdReq[A]) => Future[Result]): Future[Result] = {
        val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)

        val personIdOpt = sessionUtil.getPersonId(request)
        val user = mSioUsers(personIdOpt)

        if (user.isSuper) {
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

      def madNotFound(request: Request[_]): Future[Result] = {
        Results.NotFound("ad not found: " + adId)
      }

    }
  }

}

trait IIsSuMad {
  val isSuMad: IsSuMad
}
