package util.acl

import controllers.SioController
import models.req.SioReqMd
import play.api.mvc._
import util.di.IIdentUtil
import scala.concurrent.Future
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 17:42
 * Description: Является ли текущий юзер НЕзалогиненным (анонимусом)?
 */
trait IsAnonCtl extends SioController with IIdentUtil {

  trait IsAnonBase extends ActionBuilder[AbstractRequestWithPwOpt] {
    override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      pwOpt match {
        case None =>
          val srm = SioReqMd(usernameOpt = None)
          val req1 = RequestWithPwOpt(pwOpt, request, srm)
          block(req1)

        case Some(pw) =>
          identUtil.redirectUserSomewhere(pw.personId)
      }
    }
  }

  abstract class IsAnonBase2
    extends IsAnonBase
    with ExpireSession[AbstractRequestWithPwOpt]

  // CSRF:
  /** GET-запросы с выставлением CSRF-токена. */
  object IsAnonGet
    extends IsAnonBase2
    with CsrfGet[AbstractRequestWithPwOpt]

  /** POST-запросы с проверкой CSRF-токена, выставленного ранее через [[IsAnonGet]] или иной [[CsrfGet]]. */
  object IsAnonPost
    extends IsAnonBase2
    with CsrfPost[AbstractRequestWithPwOpt]

}
