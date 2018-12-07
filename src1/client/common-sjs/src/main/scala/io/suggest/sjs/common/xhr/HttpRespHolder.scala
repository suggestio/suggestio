package io.suggest.sjs.common.xhr

import java.nio.ByteBuffer

import boopickle.Pickler
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.ex.XhrFailedException
import org.scalajs.dom.XMLHttpRequest
import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import japgolly.univeq._

import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 12:55
  * Description: Модель асинхронного http-ответа, абстрагированная от нижележащего http-механизма.
  * Содержит фьючерс внутри.
  */
sealed trait HttpRespHolder {

  def successIfStatusF(isOkF: Int => Boolean): HttpRespHolder

  def responseTextFut: Future[String]

  /** Сырой ответ согласно respType. */
  def responseRawFut: Future[js.Any]

  /** Вернуть статус ответа сервера. */
  def responseStatusFut: Future[Int]

  /** Любой фьючерс ответа на запрос, содержимое не важно.
    * По идее, без доп. Future() внутри: быстрее и короче, чем остальные методы.
    */
  def future: Future[_]

}
object HttpRespHolder {

  /** Статическая утиль классов, независящая от конкретной реализации. */
  implicit class HrhExtOps( val underlying: HttpRespHolder ) extends AnyVal {

    /**
      * Фильтровать результат по http-статусу ответа сервера.
      *
      * @param httpStatuses Допустимые http-статусы.
      * @param xhrFut Выполненяемый XHR, собранный в send().
      * @return Future, где success наступает только при указанных статусах.
      *         [[io.suggest.sjs.common.xhr.ex.XhrFailedException]] когда статус ответа не подпадает под критерий.
      */
    def successIfStatus(httpStatuses: Int*) = underlying.successIfStatusF( httpStatuses.contains )

    def successIf200 = successIfStatus( HttpStatuses.OK )

    def successIfStatusStartsWith00X(statusPrefix: Int) =
      underlying.successIfStatusF(_ / 10 ==* statusPrefix)
    def successIf30X = successIfStatusStartsWith00X(30)
    def successIf20X = successIfStatusStartsWith00X(20)


    /** Вернуть сырой нативный json, скастовав его к указанному типу (js.Dynamic обычно бесполезен). */
    def nativeJsonFut[T <: js.Any]: Future[T] = {
      for (jsonText <- underlying.responseTextFut) yield
        JSON.parse( jsonText ).asInstanceOf[T]
    }

    def arrayBufferFut: Future[ByteBuffer] = {
      for (raw <- underlying.responseRawFut) yield
        TypedArrayBuffer.wrap( raw.asInstanceOf[ArrayBuffer] )
    }

    /**
      * Декодировать будущий ответ сервера в инстанс какой-то модели с помощью boopickle и десериализатора,
      * переданного в implicit typeclass'е.
      *
      * @param respFut Фьючерс с ответом сервера.
      * @tparam T Тип отрабатываемой модели.
      * @return Фьючерс с десериализованным инстансом произвольной модели.
      */
    def unBooPickle[T: Pickler]: Future[T] = {
      for (bbuf <- arrayBufferFut) yield
        PickleUtil.unpickle[T](bbuf)
    }

    def unJson[T: Reads]: Future[T] = {
      for (jsonStr <- underlying.responseTextFut) yield
        Json.parse(jsonStr).as[T]
    }

  }

}



/** Поддержка нативного XMLHttpRequest. */
case class XhrHttpRespHolder(
                              xhrFut: Future[XMLHttpRequest]
                            )
  extends HttpRespHolder
{

  def withXhrFut(xhrFut: Future[XMLHttpRequest]) = copy(xhrFut = xhrFut)

  override def successIfStatusF(isOkF: Int => Boolean): XhrHttpRespHolder = {
    withXhrFut(
      xhrFut.transformWith {
        case Success(xhr) =>
          if ( isOkF(xhr.status) ) {
            Future.successful(xhr)
          } else {
            Future.failed( XhrFailedException(xhr) )
          }
        // Ajax() сыплет exception'ами, на любые не-2хх ответы. Перехватить нежелательный exception, если status в разрешённых:
        case Failure(ex) =>
          ex match {
            case aer: XhrFailedException if isOkF(aer.xhr.status) =>
              Future.successful( aer.xhr )
            case _ =>
              Future.failed(ex)
          }
      }
    )
  }

  override def responseTextFut: Future[String] = {
    for (xhr <- xhrFut) yield
      xhr.responseText
  }

  override def responseRawFut: Future[js.Any] = {
    for (xhr <- xhrFut) yield
      xhr.response
  }

  override def responseStatusFut: Future[Int] = {
    for (xhr <- xhrFut) yield
      xhr.status
  }

  override def future: Future[_] = xhrFut

}
