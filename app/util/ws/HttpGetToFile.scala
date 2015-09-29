package util.ws

import java.io.{FileOutputStream, File}

import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.{WSClient, WSResponseHeaders}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.xplay.IteeUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 10:23
 * Description: Короткий способ асинхронно отфетчить ссылку в файл.
 */
trait HttpGetToFile {

  /** Клиент play-ws. */
  def ws: WSClient

  /** Ссылка для фетчинга. */
  def urlStr: String

  /** Следовать редиректам? */
  def followRedirects: Boolean

  /**
   * Валиден ли http-статус ответа remote-сервера по ссылке?
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
  def statusCodeInvalidException(resp: WSResponseHeaders): Exception = {
    new IllegalArgumentException("Unexpected HTTP status: " + resp.status)
  }

  /**
   * Запустить фетчинг файла на исполнение.
   * @return Future с файлом, куда отфетчен контент.
   *         Future с экзепшеном в иных случаях.
   */
  def request(): Future[(WSResponseHeaders, File)] = {
    val respFut = ws.url(urlStr)
      .withFollowRedirects(followRedirects)
      .getStream()
    respFut.flatMap { case (headers, body) =>
      if ( !isStatusValid(headers.status) ) {
        Future failed statusCodeInvalidException(headers)
      } else {
        val f = File.createTempFile(tempFilePrefix, tempFileSuffix)
        val resFut = IteeUtil.writeIntoFile(body, f)
          // Вернуть готовый файл, когда всё закончится.
          .map { _ => headers -> f }
        resFut
      }
    }
  }

}
