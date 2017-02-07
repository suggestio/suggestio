package util.acl

import com.google.inject.Inject
import models.req.MReq
import play.api.mvc._

import scala.concurrent.Future
import controllers.routes
import io.suggest.util.logs.{IMacroLogs, MacroLogsImpl}
import io.suggest.common.fut.FutureUtil.HellImplicits._
import models.mproj.ICommonDi

// TODO Сделать всё это действо injectable. Возможно даже объеденить оба трейта в один класс.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */

trait OnUnauthUtil {

  /** Основная синхронная реакция на выявленную необходимость залогинится.
    *
    * @param request реквест.
    * @return Ответ с редиректом.
    */
  def onUnauthBase(request: RequestHeader): Result = {
    val rOpt = Some(request.path)
    Results.Redirect( routes.Ident.emailPwLoginForm(r = rOpt) )
  }

  /** Что делать, когда юзер не авторизован? Асинхронная реакция. */
  def onUnauth(request: RequestHeader): Future[Result] = {
    onUnauthBase(request)
  }

}

/** Аддон для контроллеров, добавляющий поддержку IsAuth action builder'ов. */
class IsAuth @Inject() (override val mCommonDi: ICommonDi)
  extends Csrf
{

  import mCommonDi._

  trait IsAuthBase extends ActionBuilder[MReq] with IMacroLogs with OnUnauthUtil {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      if (personIdOpt.isDefined) {
        // Юзер залогинен. Продолжить выполнения экшена.
        val user = mSioUsers(personIdOpt)
        val req1 = MReq(request, user)
        block(req1)

      } else {
        LOGGER.debug("invokeBlock(): anonymous access prohibited. path = " + request.path)
        onUnauth(request)
      }
    }

  }

  /** Реализация IsAuth с возможностью задания значения поля obeyReturnPath. */
  sealed class IsAuthC
    extends IsAuthBase
    with ExpireSession[MReq]
    with MacroLogsImpl


  /** Проверка на залогиненность юзера без CSRF-дейстий. */
  object IsAuth
    extends IsAuthC
  @inline
  def apply() = IsAuth

  /** Проверка на залогиненность юзера с выставлением CSRF-токена. */
  object Get
    extends IsAuthC
    with CsrfGet[MReq]

  /** Проверка на залогиненность юзера с проверкой CSRF-токена, выставленного ранее. */
  object Post
    extends IsAuthC
    with CsrfPost[MReq]

}

trait IIsAuth {
  def isAuth: IsAuth
}

