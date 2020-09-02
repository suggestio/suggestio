package io.suggest.proto.http.model

import java.time.{LocalDate, ZoneOffset}

import io.suggest.msg.ErrorMsgs
import org.scalajs.dom
import org.scalajs.dom.experimental.serviceworkers.Cache
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.experimental.{Request, Response}

import scala.concurrent.Future
import scala.scalajs.js
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.dom2.DomWindowCaches._

import scala.scalajs.js.JavaScriptException

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 16:58
  * Description: Модель-надстройка над HTTP Caches API.
  *
  * Для чистки старых кэшей используется rolling-схема нумерации индексов по дням, и удаление старых.
  */
object MHttpCaches {

  /** Мажорная версия кэша, нарастает инкрементально: 00, 01,... 0A,... 0Z, 10,..., ZZ... */
  final def CACHE_VERSION = "00"

  /** Разделитель морфем в ключе кэша. */
  private final def CACHE_KEY_DELIM = "_"

  /** Сколько дней кэшировать максимум.*/
  final def CACHE_MAX_DAYS = 3

  private var _isAvailable: Boolean =
    dom.window.cachesOrUndef.nonEmpty
  // TODO По голому http может не работать, только по https - нужно сбрасывать в false при ошибках.


  /** Проверить доступность API хранилища кэширования. */
  def isAvailable() = _isAvailable

  /** Быстрый доступ к хранилищу. */
  def storage() = dom.window.caches


  /** Поиск закэшированного реквеста через match(). */
  def findMatching(req: Request): Future[Option[Response]] = {
    _tryAvailable { () =>
      storage()
        .`match`(req)
        .toFuture
        .map { _.asInstanceOf[js.UndefOr[Response]].toOptionNullable }
    }
  }

  /** Открыть кэш для использования. */
  def open(cacheName: String): Future[MHttpCache] = {
    _tryAvailable { () =>
      storage()
        .open( cacheName )
        .toFuture
        .map( MHttpCache.apply )
    }
  }

  /** Кэш может не работать. Например, благодаря https-only. Его нужно вырубать автоматом. */
  private def _tryAvailable[T](f: () => Future[T]): Future[T] = {
    if (isAvailable()) {
      try {
        val futRes = f()
        for (ex <- futRes.failed) {
          _isAvailable = false
          Future.failed( new UnsupportedOperationException(ErrorMsgs.CACHING_ERROR, ex) )
        }
        futRes
      } catch { case ex: JavaScriptException =>
        _isAvailable = false
        Future.failed( new IllegalStateException(ErrorMsgs.CACHING_ERROR, ex) )
      }
    } else {
      Future.failed( new NoSuchElementException(ErrorMsgs.CACHING_ERROR) )
    }
  }

  /** Обычный вызов now() требует жирную базу временных зон, её на руках нет. */
  private def _localDateNow = LocalDate.now( ZoneOffset.UTC )

  /** Ключи кэша идут по дням. */
  def cacheKey(date: LocalDate = _localDateNow): String = {
    val delim = CACHE_KEY_DELIM
    "c" + delim +
      CACHE_VERSION + delim +
      date.getYear + date.getMonthValue + date.getDayOfMonth
  }


  /** Быстрое удаление старых кэшей по rolling-ключам. */
  def gc(): Future[_] = {
    _tryAvailable { () =>
      val allCacheKeysFut = storage().keys().toFuture
      val yesterday = _localDateNow
        .minusDays( CACHE_MAX_DAYS )
      val maxKey = cacheKey( yesterday )
      for {
        allCacheKeys <- allCacheKeysFut
        if allCacheKeys.nonEmpty

        cacheKeysForDelete = allCacheKeys
          .iterator
          .filter( _ >= maxKey )
          .to( LazyList )

        if cacheKeysForDelete.nonEmpty

        results <- Future.traverse( cacheKeysForDelete ) { ck4d =>
          storage()
            .delete( ck4d )
            .toFuture
        }

      } yield {
        results
      }
    }
  }

}


/** Обёртка для конкретного кэша. */
case class MHttpCache(cache: Cache) {

  /** Запихивание ответа в кэш. */
  def put(key: Request, value: Response): Future[Unit] =
    cache.put(key, value).toFuture

}
