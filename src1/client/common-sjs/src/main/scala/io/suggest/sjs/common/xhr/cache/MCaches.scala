package io.suggest.sjs.common.xhr.cache

import java.time.LocalDate

import org.scalajs.dom
import org.scalajs.dom.Window
import org.scalajs.dom.experimental.serviceworkers.{Cache, CacheStorage}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.experimental.{Request, Response}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 16:58
  * Description: Модель-надстройка над HTTP Caches API.
  *
  * Для чистки старых кэшей используется rolling-схема нумерации индексов по дням, и удаление старых.
  */
object MCaches {

  import WindowCachesStub._

  /** Мажорная версия кэша, нарастает инкрементально: AA, AB, AC...ZZ... */
  final def CACHE_VERSION = "aa"

  /** Разделитель морфем в ключе кэша. */
  private final def CACHE_KEY_DELIM = "_"

  /** Сколько дней кэшировать максимум.*/
  final def CACHE_MAX_DAYS = 3

  private var _isAvailable: Boolean =
    dom.window.cachesOrUndef.nonEmpty
  // TODO По голому http может не работать, только по https - нужно сбрасывать в false при ошибках.

  /** Проверить доступность API хранилища кэширования. */
  def isAvailable = _isAvailable


  /** Быстрый доступ к хранилищу. */
  def storage = dom.window.caches


  /** Поиск закэшированного реквеста через match(). */
  def findMatching(req: Request): Future[Option[Response]] = {
    storage.`match`(req)
      .toFuture
      .map { _.asInstanceOf[js.UndefOr[Response]].toOptionNullable }
  }


  /** Открыть кэш для использования. */
  def open(cacheName: String): Future[MCache] = {
    storage
      .open( cacheName )
      .toFuture
      .map( MCache.apply )
  }


  /** Ключи кэша идут по дням. */
  def cacheKey(date: LocalDate = LocalDate.now()): String = {
    val delim = CACHE_KEY_DELIM
    "hc" + delim +
      CACHE_VERSION + delim +
      date.getYear + date.getMonthValue + date.getDayOfMonth
  }


  /** Быстрое удаление старых кэшей по rolling-ключам. */
  def gc(): Future[_] = {
    val allCacheKeysFut = storage.keys().toFuture
    val yesterday = LocalDate.now()
      .minusDays( CACHE_MAX_DAYS )
    val maxKey = cacheKey( yesterday )
    for {
      allCacheKeys <- allCacheKeysFut
      if allCacheKeys.nonEmpty

      cacheKeysForDelete = allCacheKeys
        .iterator
        .filter( _ >= maxKey )
        .toStream
      if cacheKeysForDelete.nonEmpty

      results <- Future.traverse( cacheKeysForDelete ) { ck4d =>
        storage
          .delete( ck4d )
          .toFuture
      }

    } yield {
      results
    }
  }

}


/** Обёртка для конкретного кэша. */
case class MCache(cache: Cache) {

  /** Запихивание ответа в кэш. */
  def put(key: Request, value: Response): Future[Unit] =
    cache.put(key, value).toFuture

}


@js.native
trait WindowCachesStub extends js.Object {

  @JSName("caches")
  val cachesOrUndef: js.UndefOr[js.Function] = js.native

  def caches: CacheStorage = js.native

}
object WindowCachesStub {
  implicit def apply( window: Window ): WindowCachesStub =
    window.asInstanceOf[WindowCachesStub]
}

