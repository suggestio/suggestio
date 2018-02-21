package io.suggest.ahc.util

import java.io.File
import javax.inject.{Inject, Singleton}

import io.suggest.streams.StreamsUtil
import play.api.http.HttpVerbs
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 10:23
 * Description: Простая утиль чтобы асинхронно и поточно скачать ссылку в файл по HTTP.
 */
@Singleton
class HttpGetToFile @Inject() (
                                ws                      : WSClient,
                                streamsUtil             : StreamsUtil,
                                implicit private val ec : ExecutionContext
                              ) {

  /** Класс для качания из интернетов. */
  abstract class AbstractDownloader {

    /** Ссылка для фетчинга. */
    def urlStr: String

    /** Следовать редиректам? */
    def followRedirects: Boolean

    /**
      * Валиден ли http-статус ответа remote-сервера по ссылке?
      *
      * @param status HTTP статус.
      * @return true, если ответ можно сохранить в файл. Иначе false.
      */
    def isStatusValid(status: Int): Boolean = {
      status == 200
    }

    /** Префикс имени временного файла. */
    def tempFilePrefix: String = getClass.getSimpleName

    /** Суффикс имени временного файла. */
    def tempFileSuffix: String = ""

    /** Какой экзепшен генерить, если http status не выдержал проверки? */
    def statusCodeInvalidException(resp: WSResponse): Exception = {
      new IllegalArgumentException("Unexpected HTTP status: " + resp.status)
    }

    /**
      * Запустить фетчинг файла на исполнение.
      *
      * @return Future с файлом, куда отфетчен контент.
      *         Future с экзепшеном в иных случаях.
      */
    def request(): Future[DlResp] = {
      val respFut = ws.url(urlStr)
        .withFollowRedirects(followRedirects)
        .withMethod(HttpVerbs.GET)
        .stream()

      respFut.flatMap { sr =>
        if (!isStatusValid(sr.status)) {
          val ex = statusCodeInvalidException(sr)
          Future.failed(ex)
        } else {
          val f = File.createTempFile(tempFilePrefix, tempFileSuffix)
          for {
            _ <- streamsUtil.sourceIntoFile(sr.bodyAsSource, f)
          } yield {
            // Вернуть готовый файл, когда всё закончится.
            DlResp(sr.headers, f)
          }
        }
      }
    }

  }


  /** Дефолтовая реализация AbstractDownloader. */
  case class Downloader(
    urlStr: String,
    followRedirects: Boolean
  )
    extends AbstractDownloader

}

/** Модель ответа от [[HttpGetToFile]].AbstractDownloader.request() */
case class DlResp(
                   headers  : Map[String, Seq[String]],
                   file     : File
                 )
