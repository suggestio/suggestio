package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.Future
import scala.language.higherKinds

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.02.15 17:56
 * Description: Поддержка CSRF-защиты в контроллерах.
 */

@Singleton
class Csrf @Inject() (mCommonDi: ICommonDi) {

  import mCommonDi.{ec, csrfAddToken, csrfCheck}

  /** Доп.заголовки, чтобы не пересобирать их при каждом запросе. */
  val HEADERS = List(
    HeaderNames.VARY          -> "Set-Cookie,Cookie",
    // Cache-Control режет токен, если НЕ пустой либо НЕ содержит no-cache.
    HeaderNames.CACHE_CONTROL -> "private, no-cache, must-revalidate"
  )

  /** Накинуть доп.заголовки на ответ по запросу. */
  def withNoCache(result: Result): Result = {
    result.withHeaders(HEADERS: _*)
  }

  private def withNoCacheFut(resFut: Future[Result]): Future[Result] = {
    resFut.map { withNoCache }
  }

  /** Аддон для action-builder'ов, добавляющий выставление CSRF-токена в сессию. */
  trait Get[R[_]] extends ActionBuilder[R] {

    abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
      withNoCacheFut {
        super.invokeBlock(request, block)
      }
    }

    override protected def composeAction[A](action: Action[A]): Action[A] = {
      csrfAddToken(super.composeAction(action))
    }
  }


  /** Аддон для action-builder'ов, добавляющий проверку CSRF-токена перед запуском экшена на исполнение. */
  trait Post[R[_]] extends ActionBuilder[R] {

    abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
      withNoCacheFut {
        super.invokeBlock(request, block)
      }
    }

    override protected def composeAction[A](action: Action[A]): Action[A] = {
      csrfCheck(super.composeAction(action))
    }
  }

}
