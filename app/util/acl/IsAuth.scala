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

trait IsAuthBase extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
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

/** Реализация IsAuth с возможностью задания значения поля obeyReturnPath. */
case class IsAuthC(obeyReturnPath: Boolean = true) extends IsAuthBase with ExpireSession[AbstractRequestWithPwOpt]


/** Проверка на залогиненность юзера без CSRF-дейстий. */
object IsAuth extends IsAuthC()

/** Проверка на залогиненность юзера с выставлением CSRF-токена. */
object IsAuthGet  extends IsAuthC() with CsrfGet[AbstractRequestWithPwOpt]

/** Проверка на залогиненность юзера с проверкой CSRF-токена, выставленного ранее. */
object IsAuthPost extends IsAuthC() with CsrfPost[AbstractRequestWithPwOpt]
