package util.acl

import controllers.SioController
import io.suggest.di.{IExecutionContext, IEsClient}
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import models.MAd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 13:44
 * Description:
 * Абстрактная логика обработки запроса суперюзера на какое-либо действие с рекламной карточкой.
 */
trait IsSuperuserMad
  extends SioController
  with IEsClient
  with IExecutionContext
  with IsSuperuserUtilCtl
{

  sealed trait IsSuperuserMadBase
    extends ActionBuilder[RequestWithAd]
    with IsSuperuserUtil
  {

    /** id запрашиваемой рекламной карточки. */
    def adId: String

    override def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[Result]): Future[Result] = {
      val madOptFut = MAd.getById(adId)
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper isSuperuser pwOpt) {
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        madOptFut flatMap {
          case Some(mad) =>
            srmFut flatMap { srm =>
              val req1 = RequestWithAd(mad, request, pwOpt, srm)
              block(req1)
            }
          case None =>
            madNotFound(request)
        }
      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

    def madNotFound(request: Request[_]): Future[Result] = {
      Future successful NotFound("ad not found: " + adId)
    }
  }


  abstract class IsSuperuserMadAbstract
    extends IsSuperuserMadBase
    with ExpireSession[RequestWithAd]


  /** ACL action builder на действия с указанной рекламной карточкой. */
  case class IsSuperuserMad(override val adId: String)
    extends IsSuperuserMadAbstract

  /** ACL action builder на действия с указанной рекламной карточкой. + CSRF выставление токена в сессию. */
  case class IsSuperuserMadGet(override val adId: String)
    extends IsSuperuserMadAbstract
    with CsrfGet[RequestWithAd]


  /** ACL action builder на действия с указанной рекламной карточкой. + Проверка CSRF-токена. */
  case class IsSuperuserMadPost(override val adId: String)
    extends IsSuperuserMadAbstract
    with CsrfPost[RequestWithAd]

}
