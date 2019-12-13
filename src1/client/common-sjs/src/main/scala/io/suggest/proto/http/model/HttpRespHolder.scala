package io.suggest.proto.http.model

import japgolly.univeq.UnivEq

import scala.concurrent.Future
import scala.scalajs.js.JavaScriptException

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:55
  * Description: Модель асинхронного http-ответа, абстрагированная от нижележащего http-механизма.
  * Содержит фьючерс внутри.
  */
trait HttpRespHolder extends IMapResult[HttpResp] {
  // Поддержка маппинга ответа сервера здесь (внутри HttpRespHolder) смысла нет,
  // т.к. abort- и progress-методы после фактического окончания http-запроса теряют смысл.

  /** Вернуть обёртку результата запроса. */
  def httpResponseFut: Future[HttpResp]

  /** Прервать запрос.
    * @throws JavaScriptException Теоретически возможно. */
  def abortOrFail(): Unit

  /** Прервать запрос, подавить возможные исключения. */
  def abort(): Unit = {
    try {
      abortOrFail()
    } catch { case ex: Throwable =>
      println(ex.toString)
    }
  }

  override def mapResult[T2](f: Future[HttpResp] => Future[T2]): HttpRespMapped[T2] = {
    HttpRespMapped(
      httpRespHolder = this,
      resultFut      = f(httpResponseFut),
    )
  }

  // TODO Поддержка progress'а (для upload'а).
  //   Для XHR всё понятно: подписка на onprogress().
  //   Для Fetch API требуются вкручивать костыли по-интереснее, что потребует изменения HttpClient API (upload progress надо будет заявлять при сборке реквеста):
  //   - https://github.com/whatwg/fetch/issues/21#issuecomment-381425704 (отсылка к спекам, требуется Request поверх ReadableStream в браузере).
  //   - https://github.com/whatwg/fetch/issues/607#issuecomment-564461907 (download-пример, НЕ для upload)

}


object HttpRespHolder {

  @inline implicit def univEq: UnivEq[HttpRespHolder] = UnivEq.force

  implicit class HrhOpsExt( val httpRespHolder: HttpRespHolder ) extends AnyVal {

    /** Вернуть обёртку результата запроса, но делать page reload, если истекла сессия. */
    def respAuthFut: Future[HttpResp] = {
      httpRespHolder
        .httpResponseFut
        .reloadIfUnauthorized()
    }

  }

}

