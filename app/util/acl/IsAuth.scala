package util.acl

import play.api.mvc._
import scala.concurrent.Future
import controllers.routes

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */
object IsAuth extends ActionBuilder[AbstractRequestWithPwOpt] {

  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    invokeBlockHelper(onUnauthDefault, request, block)
  }


  /** Что делать, когда юзер не авторизован? */
  def onUnauthDefault(req: RequestHeader): SimpleResult = {
    Results.Redirect(routes.Ident.persona())
  }


  /** Общий код статического и динамического IsAuth-генераторов. */
  def invokeBlockHelper[A](onUnauth: Request[A] => SimpleResult,  request: Request[A],  block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (pwOpt.isDefined) {
      // Юзер залогинен. Продолжить выполнения экшена.
      val req1 = new RequestWithPwOpt(pwOpt, request)
      block(req1)
    } else {
      Future.successful(onUnauth(request))
    }
  }

}


/** Если нужно редиректить в нестандартном направлении, то используется этот класс. */
case class IsAuthF(onUnauth: RequestHeader => SimpleResult) extends ActionBuilder[AbstractRequestWithPwOpt] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    IsAuth.invokeBlockHelper(onUnauth, request, block)
  }
}
