package util.acl

import play.api.mvc._
import scala.concurrent.Future
import controllers.routes
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */
object IsAuth extends IsAuthAbstract {

  /** Что делать, когда юзер не авторизован? */
  def onUnauth(req: RequestHeader): Future[Result] = {
    Future.successful(
      Results.Redirect(routes.Ident.persona())
    )
  }

}


trait IsAuthAbstract extends ActionBuilder[AbstractRequestWithPwOpt] {

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
      onUnauth(request)
    }
  }

  // Действия, когда персонаж не идентифицирован.
  def onUnauth(req: RequestHeader): Future[Result]

}


