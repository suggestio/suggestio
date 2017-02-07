package util.acl

import com.google.inject.Inject
import io.suggest.model.n2.node.MNodeTypes
import models.mproj.ICommonDi
import models.req.{MAdReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 13:44
 * Description:
 * Абстрактная логика обработки запроса суперюзера на какое-либо действие с рекламной карточкой.
 */
class IsSuMad @Inject()(override val mCommonDi: ICommonDi)
  extends Csrf
{

  import mCommonDi._

  sealed trait IsSuMadBase
    extends ActionBuilder[MAdReq]
    with IsSuUtil
  {

    /** id запрашиваемой рекламной карточки. */
    def adId: String

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
        supOnUnauthFut(req1)
      }
    }

    def madNotFound(request: Request[_]): Future[Result] = {
      Results.NotFound("ad not found: " + adId)
    }
  }


  abstract class IsSuMadAbstract
    extends IsSuMadBase
    with ExpireSession[MAdReq]


  /** ACL action builder на действия с указанной рекламной карточкой. */
  case class IsSuMad(override val adId: String)
    extends IsSuMadAbstract
  def apply(adId: String) = IsSuMad(adId)

  /** ACL action builder на действия с указанной рекламной карточкой. + CSRF выставление токена в сессию. */
  case class Get(override val adId: String)
    extends IsSuMadAbstract
    with CsrfGet[MAdReq]


  /** ACL action builder на действия с указанной рекламной карточкой. + Проверка CSRF-токена. */
  case class Post(override val adId: String)
    extends IsSuMadAbstract
    with CsrfPost[MAdReq]

}

trait IIsSuMad {
  val isSuMad: IsSuMad
}
