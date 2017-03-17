package util.acl

import com.google.inject.{Inject, Singleton}
import models.req.MReq
import play.api.mvc._

import scala.concurrent.Future
import controllers.routes
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.id.IdentConst
import io.suggest.www.util.acl.SioActionBuilderOuter
import play.api.http.{HeaderNames, MimeTypes}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */

/** Аддон для контроллеров, добавляющий поддержку IsAuth action builder'ов. */
@Singleton
class IsAuth @Inject() (
                         aclUtil                : AclUtil
                       )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  /** Основная синхронная реакция на выявленную необходимость залогинится.
    *
    * @param request реквест.
    * @return Ответ с редиректом.
    */
  def onUnauthBase(request: RequestHeader): Result = {
    val rOpt = Some(request.path)

    // Ожидает ли клиент в ответе увидеть HTML-форму? Нужно для защиты от совсем бессмыленного отвечания на запрос.
    val acceptOpt = request.headers.get( HeaderNames.ACCEPT )
    val clientAcceptsHtmlForm = acceptOpt.isEmpty || acceptOpt.exists { v =>
      v.contains("*/*") || v.contains( MimeTypes.HTML )
    }

    val url = routes.Ident.emailPwLoginForm(r = rOpt)
    if (clientAcceptsHtmlForm) {
      Results.Redirect( url )
    } else {
      LOGGER.trace(s"onUnauthBase($request): 401, because Accept: $acceptOpt")
      Results.Unauthorized
        .withHeaders( IdentConst.HTTP_HDR_SUDDEN_AUTH_FORM_RESP -> url.url )
    }
  }

  /** Что делать, когда юзер не авторизован? Асинхронная реакция. */
  def onUnauth(request: RequestHeader): Future[Result] = {
    onUnauthBase(request)
  }


  /** реализация action-builder'а с поддержкой автоматического управления сессией. */
  sealed class Impl extends SioActionBuilderImpl[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = aclUtil.userFromRequest(request)

      user.personIdOpt.fold {
        LOGGER.debug("invokeBlock(): anonymous access prohibited. path = " + request.path)
        onUnauth(request)
      } { _ =>
        // Юзер залогинен. Продолжить выполнения экшена.
        val req1 = MReq(request, user)
        block(req1)
      }
    }

  }


  /** Проверка на залогиненность юзера без CSRF-дейстий. */
  val IsAuth = new Impl

  @inline
  def apply() = IsAuth

}

/** Интерфейс для поля с DI-инстансом [[IsAuth]]. */
trait IIsAuth {
  def isAuth: IsAuth
}

