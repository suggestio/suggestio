package io.suggest.proto.http.model

import japgolly.univeq.UnivEq

import scala.concurrent.Future
import scala.scalajs.js.JavaScriptException
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:55
  * Description: Модель асинхронного http-ответа, абстрагированная от нижележащего http-механизма.
  * Содержит фьючерс внутри.
  */
trait IHttpResultHolder[T] {
  // Поддержка маппинга ответа сервера здесь (внутри HttpRespHolder) смысла нет,
  // т.к. abort- и progress-методы после фактического окончания http-запроса теряют смысл.

  def resultFut: Future[T]

  /** Прервать запрос.
    * @throws JavaScriptException Теоретически возможно. */
  def abortOrFail(): Unit

  /** Прервать запрос, подавить возможные исключения. */
  def abort(): Try[Unit] = {
    Try {
      abortOrFail()
    }
  }

  def mapResult[T2](f: Future[T] => Future[T2]): HttpRespMapped[T2]

}


object IHttpResultHolder {

  @inline implicit def univEq[T]: UnivEq[IHttpResultHolder[T]] = UnivEq.force

  implicit final class PlainHttpRespHolderOpsExt( private val httpRespHolder: IHttpResultHolder[HttpResp] ) extends AnyVal {

    /** Вернуть обёртку результата запроса, но делать page reload, если истекла сессия. */
    def respAuthFut: Future[HttpResp] = {
      httpRespHolder
        .resultFut
        .reloadIfUnauthorized()
    }

  }

}


/** Быстрая реализация [[IHttpResultHolder]] для сырого HttpResp. */
trait IHttpRespHolder extends IHttpResultHolder[HttpResp] {
  override def mapResult[T2](f: Future[HttpResp] => Future[T2]): HttpRespMapped[T2] = {
    HttpRespMapped[T2](
      httpResultHolder = this,
      resultFut        = f(resultFut),
    )
  }
}
