package util.acl

import play.api.mvc._
import scala.concurrent.Future
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 17:42
 * Description: Является ли текущий юзер НЕзалогиненным (анонимусом)?
 */
trait IsAnonBase extends ActionBuilder[AbstractRequestWithPwOpt] {
  override protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case None =>
        val srm = SioReqMd(usernameOpt = None)
        val req1 = RequestWithPwOpt(pwOpt, request, srm)
        block(req1)

      case Some(pw) =>
        controllers.Ident.redirectUserSomewhere(pw.personId)
    }
  }
}

/** Реализация [[IsAnonBase]] с поддержкой [[ExpireSession]]. Такое необходимо, чтобы
  * в функциях логина выставлялся таймер после выставления personId в контроллере. */
object IsAnon extends IsAnonBase with ExpireSession[AbstractRequestWithPwOpt]
