package io.suggest.proto.http.client.cache

import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.adp.fetch.{FetchHttpResp, FetchRequestInit}
import io.suggest.proto.http.model.{HttpReq, MHttpCaches}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil._
import org.scalajs.dom.experimental.{Request, Response}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

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
    if (MHttpCaches.isAvailable)
      DomQuick.setTimeout(15.seconds.toMillis) { () =>
        // Повторно проверяем кэш, т.к. он мог отвалится за это время, т.к. после запуска были запросы-ответы.
        if (MHttpCaches.isAvailable)
          MHttpCaches.gc()
      }
  }


  /** Высокоуровневое API для организации кэширования на стороне JS.
    *
    * @param httpReq Исходный http-реквест.
    * @param request Скомпиленные данные будущего реквеста.
    * @param mkRequest Функция запуска реквеста на исполнение. Обычно просто вызов fetch().
    *                  Запускаемый реквест передаётся первым аргументом.
    * @return
    */
  def processCaching(httpReq: HttpReq, requestInit: FetchRequestInit)
                    (mkRequest: (Request) => Future[FetchHttpResp]): Future[FetchHttpResp] = {

    // Запуск исходного реквеста.
    lazy val __networkRespFut: Future[FetchHttpResp] = {
      val request = new Request( httpReq.url, requestInit.toRequestInit )
      mkRequest(request)
    }

    lazy val cachedReq = {
      val cachedUrl = httpReq.data.cache.rewriteUrl getOrElse httpReq.url
      // Ключ - это реквест.
      new Request( cachedUrl, requestInit.toRequestInit )
    }

    // Запуск поиска в кэше.
    lazy val __cachedRespOptFut: Future[Option[FetchHttpResp]] = {
      if (MHttpCaches.isAvailable) {
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

    // Сохранить в кэш. Может вернуть экзепшен, когда статус ответа слишком неожиданный.
    def __saveToCacheFut(resp: Response): Future[_] = {
      if ( MHttpCaches.isAvailable ) {
        val S = HttpConst.Status
        if (resp.status < S.OK  ||  resp.status >= S.MULTIPLE_CHOICES) {
          // Нельзя кэшировать ошибочные ответы, редиректы и прочее.
          Future.failed( new NoSuchElementException(resp.status.toString) )
        } else {
          try {
            val cacheKey = MHttpCaches.cacheKey()
            val fut = MHttpCaches
              .open(cacheKey)
              .flatMap(_.put(cachedReq, resp))
            // Логгирование, т.к. результат этой функции обычно дропается.
            for (ex <- fut.failed)
              LOG.info(ErrorMsgs.CACHING_ERROR, ex = ex)
            fut
          } catch {
            case ex: Throwable =>
              LOG.info(ErrorMsgs.CACHING_ERROR, ex = ex)
              Future.failed(ex)
          }
        }
      } else {
        Future.failed( new NoSuchElementException )
      }
    }

    // Разобраться, надо ли кэшировать что-либо.
    httpReq.data.cache.policy match {

      case MHttpCachingPolicies.NetworkOnly =>
        __networkRespFut


      case MHttpCachingPolicies.NetworkFirst =>
        val netRespFut = __networkRespFut
        if (MHttpCaches.isAvailable) {
          netRespFut
            .map { netResp =>
              val resp4cache = netResp.resp.clone()
              Future {
                __saveToCacheFut(resp4cache)
              }
              netResp
            }
            .recoverWith { case ex: Throwable =>
              __cachedRespOptFut.flatMap {
                case Some(resp) => Future.successful(resp)
                case None       => Future.failed(ex)
              }
            }
        } else {
          netRespFut
        }


      case MHttpCachingPolicies.Fastest =>
        if (MHttpCaches.isAvailable) {
          val cachedResOptFut = __cachedRespOptFut

          val p = Promise[FetchHttpResp]()

          val netResFut = for (netRes <- __networkRespFut) yield {
            // Клонировать запрос для сохранения в кэш:
            val respForCaching = if (p.isCompleted) netRes.resp
                                 else netRes.resp.clone()
            // Отправить ответ в кэш:
            Future {
              __saveToCacheFut( respForCaching )
            }
            netRes
          }

          for (cachedResOpt <- cachedResOptFut) {
            if (!p.isCompleted)
              for (cachedRes <- cachedResOpt)
                p.success( cachedRes )
          }
          p.completeWith( netResFut )

          p.future

        } else {
          __networkRespFut
        }


      case MHttpCachingPolicies.CacheFirst =>
        if (MHttpCaches.isAvailable) {
          __cachedRespOptFut
            .map(_.get)
            .recoverWith { case _: Throwable =>
              for (netResp <- __networkRespFut) yield {
                val resp2 = netResp.resp.clone()
                Future {
                  __saveToCacheFut( resp2 )
                }
                netResp
              }
            }

        } else {
          __networkRespFut
        }


      case MHttpCachingPolicies.CacheOnly =>
        __cachedRespOptFut
          .map(_.get)

    }
  }

}
