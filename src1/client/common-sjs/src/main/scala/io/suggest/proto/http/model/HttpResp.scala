package io.suggest.proto.http.model

import boopickle.Pickler
import io.suggest.id.IdentConst
import io.suggest.pick.PickleUtil
import io.suggest.proto.http.HttpConst
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import japgolly.univeq._
import play.api.libs.json.{Json, Reads}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 21:26
  * Description: Интерфейс ответа сервера для любой нижележащей реализации.
  */

trait HttpResp {

  /** Статус http-ответа. */
  def status: Int

  /** Текстовый статус HTTP-ответа. */
  def statusText: String

  /** Ответ из кэша? */
  def isFromInnerCache: Boolean

  /** Доступ к заголовкам ответа. */
  def getHeader(headerName: String): Option[String]


  // Следует рассматривать как одноразовые методы:

  /** Тело ответа уже прочитано? */
  def bodyUsed: Boolean

  /** Извлечение текста. */
  def text(): Future[String]

  /** Извлечь в виде ArrayBuffer. */
  def arrayBuffer(): Future[ArrayBuffer]

}


object HttpResp {

  implicit class HttpRespFutOpsExt( val httpRespFut: Future[HttpResp] ) extends AnyVal {

    /** Фильтрация по статусу с помощью функции. */
    def successIfStatusF(isOkF: Int => Boolean): Future[HttpResp] = {
      httpRespFut.transformWith {
        case Success(resp) =>
          if ( isOkF(resp.status) ) {
            Future.successful(resp)
          } else {
            Future.failed( HttpFailedException(Some(resp)) )
          }
        // Ajax() сыплет exception'ами, на любые не-2хх ответы. Перехватить нежелательный exception, если status в разрешённых:
        case Failure(ex) =>
          ex match {
            case aer: HttpFailedException if aer.resp.exists { r => isOkF(r.status) } =>
              Future.successful( aer.resp.get )
            case _ =>
              Future.failed(ex)
          }
      }
    }

    /**
      * Фильтровать результат по http-статусу ответа сервера.
      *
      * @param httpStatuses Допустимые http-статусы.
      * @param xhrFut Выполненяемый XHR, собранный в send().
      * @return Future, где success наступает только при указанных статусах.
      *         [[HttpFailedException]] когда статус ответа не подпадает под критерий.
      */
    def successIfStatus(httpStatuses: Int*): Future[HttpResp] =
      successIfStatusF( httpStatuses.contains )

    def successIf200 = successIfStatus( HttpConst.Status.OK )

    def successIfStatusStartsWith00X(statusPrefix: Int) =
      successIfStatusF(_ / 10 ==* statusPrefix)
    def successIf30X = successIfStatusStartsWith00X(30)
    def successIf20X = successIfStatusStartsWith00X(20)

    def responseTextFut: Future[String] = {
      for {
        resp      <- httpRespFut
        if !resp.bodyUsed
        jsonText  <- resp.text()
      } yield {
        jsonText
      }
    }

    /** Вернуть сырой нативный json, скастовав его к указанному типу (js.Dynamic обычно бесполезен). */
    def nativeJsonFut[T <: js.Any]: Future[T] = {
      for {
        jsonText  <- responseTextFut
      } yield {
        JSON.parse( jsonText )
          .asInstanceOf[T]
      }
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
      for {
        resp <- httpRespFut
        if !resp.bodyUsed
        ab <- resp.arrayBuffer
      } yield {
        val bbuf = TypedArrayBuffer.wrap( ab )
        PickleUtil.unpickle[T](bbuf)
      }
    }

    /** Десериализация JSON через play-json. */
    def unJson[T: Reads]: Future[T] = {
      for {
        jsonText <- responseTextFut
      } yield {
        Json
          .parse(jsonText)
          .as[T]
      }
    }

    /** Перезагружать страницу, если сервер ответил, что юзер неавторизован. */
    def reloadIfUnauthorized(): Future[HttpResp] = {
      httpRespFut.transformWith {
        // Прослушать как-будто-бы-положительные XHR-ответы, чтобы выявлять редиректы из-за отсутствия сессии.
        case Success(resp) =>
          if (
            // TODO Удалить эту строку, выставив в fetch redirect=error, и чтобы сервер это отрабатывал корректно. Сейчас redirect=follow, повторяется логика XHR.
            (resp.status ==* HttpConst.Status.OK) &&
            resp.getHeader(IdentConst.HTTP_HDR_SUDDEN_AUTH_FORM_RESP).nonEmpty
          ) {
            // Пришла HTML-форма в ответе. Такое бывает, когда сессия истекла, но "Accept:" допускает HTML-ответы.
            DomQuick.reloadPage()
            Future.failed( HttpFailedException(Some(resp)) )
          } else {
            Future.successful(resp)
          }

        case Failure(ex) =>
          ex match {
            case hfex: HttpFailedException =>
              if (hfex.resp.exists { r => r.status ==* HttpConst.Status.UNAUTHORIZED }) {
                // 401 в ответе означает, что сессия истекла и продолжать нормальную работу невозможно.
                DomQuick.reloadPage()
              }

            case _ => // Do nothing
          }
          Future.failed(ex)
      }
    }

  }

}
