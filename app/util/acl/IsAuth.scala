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
object IsAuth extends IsAuthAbstract {

  /** Что делать, когда юзер не авторизован? */
  def onUnauth(req: RequestHeader): Future[SimpleResult] = {
    Future.successful(
      Results.Redirect(routes.Ident.persona())
    )
  }

}


trait IsAuthAbstract extends ActionBuilder[AbstractRequestWithPwOpt] {

  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (pwOpt.isDefined) {
      // Юзер залогинен. Продолжить выполнения экшена.
      val req1 = new RequestWithPwOpt(pwOpt, request)
      block(req1)
    } else {
      onUnauth(request)
    }
  }

  // Действия, когда персонаж не идентифицирован.
  def onUnauth(req: RequestHeader): Future[SimpleResult]

}


