package util.acl

import play.api.mvc._
import util.PlayMacroLogsImpl
import scala.concurrent.Future
import controllers.routes
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */

trait IsAuthAbstract extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
  import LOGGER._

  /** Подчинятся редиректу назад? Если false, то юзер будет куда-то отредиректен, заведомо неизвестно куда. */
  def obeyReturnPath: Boolean

  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
    if (pwOpt.isDefined) {
      // Юзер залогинен. Продолжить выполнения экшена.
      sioReqMdFut flatMap { sioReqMd =>
        val req1 = new RequestWithPwOpt(pwOpt, request, sioReqMd)
        block(req1)
      }
    } else {
      debug("invokeBlock(): anonymous access prohibited. path = " + request.path)
      onUnauth(request)
    }
  }

  def onUnauthBase(request: RequestHeader): Result = {
    val r = if (obeyReturnPath) Some(request.path) else None
    Results.Redirect(routes.Ident.emailPwLoginForm(r = r))
  }

  /** Что делать, когда юзер не авторизован? */
  def onUnauth(request: RequestHeader): Future[Result] = {
    Future successful onUnauthBase(request)
  }

}

case class IsAuthC(obeyReturnPath: Boolean)
  extends IsAuthAbstract
  with ExpireSession[AbstractRequestWithPwOpt]

object IsAuth extends IsAuthC(true)
