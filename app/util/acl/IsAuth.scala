package util.acl

import io.suggest.di.IExecutionContext
import models.req.SioReqMd
import play.api.mvc.{Request, ActionBuilder, Result, RequestHeader}
import util.{PlayMacroLogsI, PlayMacroLogsImpl}
import scala.concurrent.Future
import controllers.{SioController, routes}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 17:26
 * Description: Убедится, что юзер является авторизованным пользователем. Иначе - отправить на страницу логина или в иное место.
 */

trait OnUnauthUtilCtl extends SioController {
  trait OnUnauthUtil {
    /** Подчинятся редиректу назад? Если false, то юзер будет куда-то отредиректен, заведомо неизвестно куда. */
    def obeyReturnPath: Boolean = true

    def onUnauthBase(request: RequestHeader): Result = {
      val r = if (obeyReturnPath) Some(request.path) else None
      Redirect( routes.Ident.emailPwLoginForm(r = r) )
    }

    /** Что делать, когда юзер не авторизован? */
    def onUnauth(request: RequestHeader): Future[Result] = {
      Future successful onUnauthBase(request)
    }
  }
}


/** Аддон для контроллеров, добавляющий поддержку IsAuth action builder'ов. */
trait IsAuth extends OnUnauthUtilCtl with IExecutionContext {

  trait IsAuthBase extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsI {

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
        LOGGER.debug("invokeBlock(): anonymous access prohibited. path = " + request.path)
        onUnauth(request)
      }
    }

    /** Подчинятся редиректу назад? Если false, то юзер будет куда-то отредиректен, заведомо неизвестно куда. */
    def obeyReturnPath: Boolean = true

    def onUnauthBase(request: RequestHeader): Result = {
      val r = if (obeyReturnPath) Some(request.path) else None
      Redirect( routes.Ident.emailPwLoginForm(r = r) )
    }

    /** Что делать, когда юзер не авторизован? */
    def onUnauth(request: RequestHeader): Future[Result] = {
      Future successful onUnauthBase(request)
    }

  }

  /** Реализация IsAuth с возможностью задания значения поля obeyReturnPath. */
  sealed class IsAuthC
    extends IsAuthBase
    with ExpireSession[AbstractRequestWithPwOpt]
    with PlayMacroLogsImpl


  /** Проверка на залогиненность юзера без CSRF-дейстий. */
  object IsAuth
    extends IsAuthC

  /** Проверка на залогиненность юзера с выставлением CSRF-токена. */
  object IsAuthGet
    extends IsAuthC
    with CsrfGet[AbstractRequestWithPwOpt]

  /** Проверка на залогиненность юзера с проверкой CSRF-токена, выставленного ранее. */
  object IsAuthPost
    extends IsAuthC
    with CsrfPost[AbstractRequestWithPwOpt]

}


