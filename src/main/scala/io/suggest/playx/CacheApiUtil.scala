package io.suggest.playx

import com.google.inject.Inject
import play.api.cache.CacheApi

import scala.concurrent.duration.Duration
import scala.concurrent.{Promise, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 21:00
 * Description: Аналог CacheUtil для DI-интерфейс play Cache.
 */
class CacheApiUtil @Inject() (cache: CacheApi) {

  /**
   * Слегка ускоренный CacheApi.getOrElse(), ориентированный на async-работу.
   * @param key Ключ кеша.
   * @param expiration Время истечения
   * @param f Функция запуска асинхронной логики.
   * @tparam T Тип результата.
   * @return Фьючерс с результатом.
   */
  def getOrElseFut[T](key: String, expiration: Duration)(f: => Future[T]): Future[T] = {
    val p = Promise[T]()
    val pfut = p.future
    val resFut: Future[T] = cache.get [Future[T]] (key) match {
      case None =>
        cache.set(key, pfut, expiration)
        f
      case Some(fut) =>
        fut
    }
    p completeWith resFut
    pfut
  }

}
