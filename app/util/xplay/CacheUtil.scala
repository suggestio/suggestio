package util.xplay

import play.api.Application
import play.api.cache.Cache

import scala.concurrent.{Promise, Future}
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.03.15 17:39
 * Description: Утиль для play-кеша.
 */
object CacheUtil {

  /**
   * Асинхронная реализация Cache.getOrElse().
   * Используется для максимально быстрого закидывания в кеш асихронных данных, которые ещё даже не запрошены.
   * По сути, кешируются фьючерсы.
   * @param ck Ключ кеша.
   * @param expirationSec Секунды кеширования.
   * @param f Генерация значения, которое будет возвращено и закинуто в кеш.
   * @tparam T Тип значения, с которым работаем.
   * @return Фьючерс с результатом.
   */
  def getOrElse[T](ck: String, expirationSec: Int)(f: => Future[T])(implicit app: Application, ct: ClassTag[T]): Future[T] = {
    val p = Promise[T]()
    val pfut = p.future
    val resFut: Future[T] = Cache.getAs [Future[T]] (ck) match {
      case None =>
        Cache.set(ck, pfut, expiration = expirationSec)
        f
      case Some(fut) =>
        fut
    }
    p completeWith resFut
    pfut
  }

}
