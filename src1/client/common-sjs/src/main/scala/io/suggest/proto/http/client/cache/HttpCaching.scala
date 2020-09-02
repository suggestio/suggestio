package io.suggest.proto.http.client.cache

import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.fetch.FetchHttpResp
import io.suggest.proto.http.model.{HttpResp, IHttpRespHolder, MHttpCaches}
import io.suggest.log.Log
import io.suggest.proto.http.client.adp.HttpAdpInstance
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.sjs.dom2.DomQuick
import org.scalajs.dom.experimental.Request

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.18 15:48
  * Description: Поддержка кэширования http-ответов на стороне js.
  * Кэш на стороне SW неудобен, т.к. не работает в cordova вообще и недостаточно контроллируется.
  * Т.к. caches API завязано на Fetch API, XHR-запросов это всё не касается.
  */
object HttpCaching extends Log {

  // Конструктор вызывается один раз.
  // Нужно выполнить отложенный gc() для удаления старых кэшей.
  // Отложенный - на случай отсутствия данных на старте.
  // TODO Надо чистить кэш после завершения *уже*успешных* запросов. Т.е. только в онлайне. И далее 1-2-3 раза сутки по таймеру до закрытия вкладки.
  Future {
    if ( MHttpCaches.isAvailable() )
      DomQuick.setTimeout(15.seconds.toMillis.toInt) { () =>
        // Повторно проверяем кэш, т.к. он мог отвалится за это время, т.к. после запуска были запросы-ответы.
        if ( MHttpCaches.isAvailable() )
          MHttpCaches.gc()
      }
  }


  /** Высокоуровневое API для организации кэширования на стороне JS.
    *
    * @param httpAdpInst Инстанс от http-адаптера.
    * @return RespHolder.
    */
  def processCaching(httpAdpInst: HttpAdpInstance): IHttpRespHolder = {
    // Запуск исходного реквеста.
    lazy val __networkRespFut: Future[HttpResp] =
      httpAdpInst.doRequest( httpAdpInst.httpReq.reqUrl )

    lazy val cachedReq: Request = {
      val cachedUrl = httpAdpInst.httpReq.origReq.data.cache.rewriteUrl
        .getOrElse( httpAdpInst.httpReq.origReq.url )
      // Ключ в браузерном кэше - это готовый реквест:
      new Request( cachedUrl, httpAdpInst.toRequestInit )
    }

    lazy val isCacheAvailable = MHttpCaches.isAvailable()

    // Запуск поиска в кэше.
    lazy val __cachedRespOptFut: Future[Option[FetchHttpResp]] = {
      if ( isCacheAvailable ) {
        for {
          respOpt <- MHttpCaches.findMatching( cachedReq )
        } yield {
          for (resp <- respOpt) yield
            FetchHttpResp(resp, isFromInnerCache = true)
        }
      } else {
        // Нет кэша - нет проблем.
        Future.successful( None )
      }
    }

    /** Сохранить в кэш. Может вернуть экзепшен, когда статус ответа слишком неожиданный
      *
      * @param netResp Полученный от сервера ответ.
      * @return Опциональный фьючерс.
      */
    def __saveToCacheFut(netResp: HttpResp): Option[Future[Unit]] =
      for {
        respDirty <- netResp.toDomResponse()
        statuses = HttpConst.Status
        if isCacheAvailable &&
           // Нельзя кэшировать ошибочные ответы, редиректы и прочее.
           respDirty.status >= statuses.OK &&
           respDirty.status < statuses.MULTIPLE_CHOICES
        resp = respDirty.clone()
        cacheKey = MHttpCaches.cacheKey()
        openCacheFut <- {
          val tryOpenCacheFut = Try( MHttpCaches.open(cacheKey) )
          for (ex <- tryOpenCacheFut.failed)
            logger.info(ErrorMsgs.CACHING_ERROR, msg = ex.toString)
          tryOpenCacheFut.toOption
        }
      } yield {
        val saveCacheFut = openCacheFut
          .flatMap(_.put(cachedReq, resp))
        // Логгирование, т.к. результат этой функции обычно дропается.
        for (ex <- saveCacheFut.failed)
          logger.info(ErrorMsgs.CACHING_ERROR, msg = ex.toString)
        saveCacheFut
      }

    // Разобраться, надо ли кэшировать что-либо.
    val respFut = httpAdpInst.httpReq.origReq.data.cache.policy match {

      case MHttpCachingPolicies.NetworkOnly =>
        __networkRespFut


      case MHttpCachingPolicies.NetworkFirst =>
        val netRespFut = __networkRespFut
        if ( isCacheAvailable ) {
          // Вернуть ответ из кэша, если ошибка:
          val resFut = netRespFut.recoverWith { case ex: Throwable =>
            __cachedRespOptFut.flatMap {
              case Some(resp) => Future.successful(resp)
              case None       => Future.failed(ex)
            }
          }

          // В фоне запустить запись респонса в кэш:
          netRespFut foreach __saveToCacheFut

          resFut
        } else {
          netRespFut
        }


      case MHttpCachingPolicies.Fastest =>
        if ( isCacheAvailable ) {
          val netResFut = __networkRespFut
          val cachedResOptFut = __cachedRespOptFut
          val p = Promise[HttpResp]()

          for (cachedResOpt <- cachedResOptFut) {
            for (cachedRes <- cachedResOpt)
              p.trySuccess( cachedRes )
          }
          p.tryCompleteWith( netResFut )

          // Закэшировать ответ сервера:
          __networkRespFut foreach __saveToCacheFut

          p.future

        } else {
          __networkRespFut
        }


      case MHttpCachingPolicies.CacheFirst =>
        if ( isCacheAvailable ) {
          __cachedRespOptFut
            .map(_.get)
            .recoverWith { case _: Throwable =>
              val netRespFut = __networkRespFut
              netRespFut foreach __saveToCacheFut
              netRespFut
            }

        } else {
          __networkRespFut
        }


      case MHttpCachingPolicies.CacheOnly =>
        __cachedRespOptFut
          .map(_.get)

    }

    httpAdpInst.toRespHolder( respFut )
  }

}
