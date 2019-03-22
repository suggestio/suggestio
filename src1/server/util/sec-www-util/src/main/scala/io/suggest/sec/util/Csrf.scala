package io.suggest.sec.util

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc._
import play.filters.csrf.{CSRFAddToken, CSRFCheck}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.15 17:56
  * Description: Поддержка CSRF-защиты в HTTP-контроллерах.
  */

@Singleton
class Csrf @Inject() (
                       csrfAddToken            : CSRFAddToken,
                       csrfCheck               : CSRFCheck,
                       defaultActionBuilder    : DefaultActionBuilder,
                       implicit private val ec : ExecutionContext
                     ) {


  /** Доп.заголовки, чтобы не пересобирать их при каждом запросе. */
  val HEADERS = {
    HeaderNames.VARY -> "Set-Cookie,Cookie" ::
      // Cache-Control подавляет токен внутри CSRFAddToken.
      // Но только если хидер НЕ пустой, либо НЕ содержит no-cache.
      HeaderNames.CACHE_CONTROL -> "private, no-cache, must-revalidate" ::
      Nil
  }

  /** Накинуть доп.заголовки на ответ по запросу. */
  def withNoCache(result: Result): Result = {
    result.withHeaders(HEADERS: _*)
  }

  private def withNoCacheFut(resFut: Future[Result]): Future[Result] = {
    resFut.map { withNoCache }
  }


  // ---------- v2 api -----------
  // Без лишнего ООП и кривого наследования.

  /** Выставление в ответ http-заголовков, запрещающих любое кэширование.
    * Исторически, были какие-то проблемы с CSRF, когда браузер неявно кэшировал какой-то
    * ответ сервера (например, между запусками браузера), а сессия уже истекала к тому времени.
    * Явное запрещение кэша решило проблему.
    * Но это было давно, может быть уже неактуально?
    */
  private def ForceNoClientCache[A](action: Action[A]): Action[A] = {
    defaultActionBuilder.async(action.parser) { request =>
      withNoCacheFut( action(request) )
    }
  }


  /** Выставить CSRF-токен в обрабатываемый реквест. */
  def AddToken[A](action: Action[A]): Action[A] = {
    csrfAddToken {
      ForceNoClientCache {
        action
      }
    }
  }


  /** Проверить CSRF-токен в обрабатываемом реквесте. */
  def Check[A](action: Action[A]): Action[A] = {
    csrfCheck {
      ForceNoClientCache {
        action
      }
    }
  }

}
