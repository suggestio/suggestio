package io.suggest.playx

import javax.inject.Inject

import io.suggest.common.fut.FutureUtil
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 21:00
 * Description: Аналог CacheUtil для DI-интерфейс play Cache.
 */
class CacheApiUtil @Inject() (
                               cache                    : AsyncCacheApi,
                               implicit private val ec  : ExecutionContext
                             ) {

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

    val resFut: Future[T] = cache.get[Future[T]](key).flatMap {
      case None =>
        // Сразу сохранить в кеш будущий фьючерс
        cache.set(key, pfut, expiration)
        // Аккуратно начать вычисление кешируемого результата.
        FutureUtil.tryCatchFut(f)

      case Some(fut) =>
        fut
    }

    p.completeWith( resFut )
    pfut
  }


  /** Если нет готового значения в кэше, то вернуть Promise, сохраняемый прямо сейчас в кэше.
    * Есть есть готовое значение, то вернуть Future.
    *
    * @param key Ключ в кэше.
    * @param expiration Время истечения.
    * @tparam T Тип значения.
    * @return Either с двумя возможными исходами.
    */
  def promiseOrCached[T](key: String, expiration: Duration): Future[Either[Promise[T], T]] = {
    cache.get[Future[T]](key).flatMap {
      case None =>
        val p = Promise[T]()
        val pfut = p.future
        cache.set(key, pfut, expiration)
        val left = Left( p )
        Future.successful(left)

      case Some(vfut) =>
        vfut.map(Right.apply)
    }
  }

}
