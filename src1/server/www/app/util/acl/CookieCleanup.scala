package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import play.api.mvc.{ActionBuilder, DiscardingCookie, Request, Result}

import scala.concurrent.Future
import scala.language.higherKinds

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.14 15:45
 * Description: Вычищать всякие _boss_session и прочие куки, которые уже не нужны.
 */

@Singleton
class CookieCleanup @Inject() (mCommonDi: ICommonDi) {

  import mCommonDi._


  /** Какие кукисы удалять? Можно задать через конфиг список имён. По умолчанию выпиливается только _boss_session. */
  val BAD_NAMES = Set(
    "_boss_session"
  )

  /** Подмешивание этого трейта к action-builder'ам позволяет запустить чистку кукисов. */
  trait CookieCleanup[R[_]] extends ActionBuilder[R] {

    abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
      // Сразу в фоне запускаем на выполнение исходный экшен.
      val superFut = super.invokeBlock(request, block)
      if (BAD_NAMES.isEmpty) {
        // Ничего фильтровать не надо.
        superFut
      } else {
        // Паралельно с выполнением реквеста ищем мусорные кукисы в реквесте.
        val garbageCookiesIter = request.cookies
          .toStream
          .map(_.name)
          .filter(BAD_NAMES.contains)
        if (garbageCookiesIter.isEmpty) {
          // Мусора нет. Возвращаем исходный результат.
          superFut
        } else {
          val dcs = garbageCookiesIter
            .map { DiscardingCookie(_) }
          superFut map {
            _.discardingCookies(dcs: _*)
          }
        }
      }
    }
  }

}
