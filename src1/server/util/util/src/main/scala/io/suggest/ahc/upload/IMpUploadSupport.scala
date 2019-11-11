package io.suggest.ahc.upload

import java.io.File

import io.suggest.ahc.util.NingUtil.ningFut2wsScalaFut
import io.suggest.common.fut.FutureUtil
import io.suggest.di.{IExecutionContext, IWsClient}
import io.suggest.pick.MimeConst
import io.suggest.util.logs.IMacroLogs
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.{FilePart, Part}
import play.api.http.HeaderNames
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 14:00
 * Description: Если сервис поддерживает загрузку данных (картинок карточек, например), то
 * тут описывается поддержка этого дела.
 */


trait IMpUploadSupport {

  /**
   * Запустить загрузку описанных данных на сервер.
   * @param args Аргументы для аплоада.
   * @return Фьючерс с ответом сервера.
   */
  def mpUpload(args: MpUploadArgs): Future[WSResponse]

  def uploadArgsSimple(file: File, ct: String, url: Option[String], fileName: String,
                       oa1AcTok: Option[RequestToken] = None): MpUploadArgs = {
    val upPart = new FilePart(mpFieldNameDflt, file, ct, null, fileName)
    MpUploadArgs(
      parts = Seq(upPart),
      url   = url,
      oa1AcTok = oa1AcTok
    )
  }

  /** Имя части для simple-загрузки. */
  def mpFieldNameDflt: String

}


/** Дефолтовая реализация multi-part upload. */
trait MpUploadSupportDflt
  extends IMpUploadSupport
    with IMacroLogs
    with IExecutionContext
    with IWsClient
{

  /**
   * Узнать URL для запроса. Аргументы содержат только опциональный URL, если он динамический.
   * @param args Аргументы upload.
   * @return
   */
  def getUploadUrl(args: MpUploadArgs): String

  /** Создание экземпляра нового реквеста. */
  def newRequest(args: MpUploadArgs, client: AsyncHttpClient) = {
    client.preparePost( getUploadUrl(args) )
      .setHeader(HeaderNames.CONTENT_TYPE, MimeConst.MULTIPART_FORM_DATA)
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  def isRespOk(args: MpUploadArgs, resp: WSResponse): Boolean

  /** Запуск HTTP-запроса. */
  def mkRequest(args: MpUploadArgs): Future[WSResponse] = {
    // TODO Play 2.5 использовать play.api.ws MultiPart вместо прямого дерганья http-клиента.
    val ningClient = wsClient.underlying[AsyncHttpClient]
    val rb = newRequest(args, ningClient)
    args.parts
      .foreach { rb.addBodyPart }
    val req = rb.build()
    val fut = ningClient.executeRequest(req)
    LOGGER.trace("Will upload to URL: " + req.getUrl)
    fut
  }

  /** Обработать запрос, отсеивая ошибки. */
  def processResponse(args: MpUploadArgs, resp: WSResponse): Future[WSResponse] = {
    if ( isRespOk(args, resp) ) {
      Future.successful(resp)
    } else {
      val msg = s"HTTP ${resp.status} ${resp.statusText}: ${resp.body}"
      Future.failed( UploadRefusedException(msg, resp) )
    }
  }

  override def mpUpload(args: MpUploadArgs): Future[WSResponse] = {
    FutureUtil.tryCatchFut {
      for {
        resp0 <- mkRequest(args)
        resp1 <- processResponse(args, resp0)
      } yield {
        resp1
      }
    }
  }

}


/** Дефолтовая реализацяи [[MpUploadArgs]]. */
case class MpUploadArgs(
                         parts    : Seq[Part],
                         url      : Option[String] = None,
                         oa1AcTok : Option[RequestToken] = None
                       )


/** Сервис отказал в аплоаде. */
case class UploadRefusedException(msg: String, wsResp: WSResponse)
  extends RuntimeException(msg)

