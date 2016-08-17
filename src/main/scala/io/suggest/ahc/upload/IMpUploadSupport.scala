package io.suggest.ahc.upload

import java.io.File

import io.suggest.ahc.util.NingUtil.ningFut2wsScalaFut
import io.suggest.common.fut.FutureUtil
import io.suggest.di.{IExecutionContext, IWsClient}
import io.suggest.util.MacroLogsI
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.request.body.multipart.{FilePart, Part}
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
  def mpUpload(args: IMpUploadArgs): Future[WSResponse]

  def uploadArgsSimple(file: File, ct: String, url: Option[String], fileName: String,
                       oa1AcTok: Option[RequestToken] = None): IMpUploadArgs = {
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
    with MacroLogsI
    with IExecutionContext
    with IWsClient
{

  /**
   * Узнать URL для запроса. Аргументы содержат только опциональный URL, если он динамический.
   * @param args Аргументы upload.
   * @return
   */
  def getUploadUrl(args: IMpUploadArgs): String

  /** Создание экземпляра нового реквеста. */
  def newRequest(args: IMpUploadArgs, client: AsyncHttpClient) = {
    client.preparePost( getUploadUrl(args) )
      .setHeader(HeaderNames.CONTENT_TYPE, "multipart/form-data")
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  def isRespOk(args: IMpUploadArgs, resp: WSResponse): Boolean

  /** Запуск HTTP-запроса. */
  def mkRequest(args: IMpUploadArgs): Future[WSResponse] = {
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
  def processResponse(args: IMpUploadArgs, resp: WSResponse): Future[WSResponse] = {
    if ( isRespOk(args, resp) ) {
      Future.successful(resp)
    } else {
      val msg = s"HTTP ${resp.status} ${resp.statusText}: ${resp.body}"
      Future.failed( UploadRefusedException(msg, resp) )
    }
  }

  override def mpUpload(args: IMpUploadArgs): Future[WSResponse] = {
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


/** Аргументы для запуска аплода. */
trait IMpUploadArgs {

  /** Части для запроса. */
  def parts     : TraversableOnce[Part]

  /** Ссылка для аплоада, если динамическая. */
  def url       : Option[String]

  /** access token, если oauth1 сервис. Иначе None. */
  def oa1AcTok  : Option[RequestToken]

}


/** Дефолтовая реализацяи [[IMpUploadArgs]]. */
case class MpUploadArgs(
  override val parts    : Traversable[Part],
  override val url      : Option[String] = None,
  override val oa1AcTok : Option[RequestToken] = None
)
  extends IMpUploadArgs


/** Сервис отказал в аплоаде. */
case class UploadRefusedException(msg: String, wsResp: WSResponse)
  extends RuntimeException(msg)

