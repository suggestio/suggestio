package util.acl

import models.req.MReq
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
      onUnauthBase(request)
    }
  }
}


/** Аддон для контроллеров, добавляющий поддержку IsAuth action builder'ов. */
trait IsAuth
  extends OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  trait IsAuthBase extends ActionBuilder[MReq] with PlayMacroLogsI {

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
    with ExpireSession[MReq]
    with PlayMacroLogsImpl


  /** Проверка на залогиненность юзера без CSRF-дейстий. */
  object IsAuth
    extends IsAuthC

  /** Проверка на залогиненность юзера с выставлением CSRF-токена. */
  object IsAuthGet
    extends IsAuthC
    with CsrfGet[MReq]

  /** Проверка на залогиненность юзера с проверкой CSRF-токена, выставленного ранее. */
  object IsAuthPost
    extends IsAuthC
    with CsrfPost[MReq]

}


