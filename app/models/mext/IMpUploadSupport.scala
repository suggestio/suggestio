package models.mext

import java.io.File

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.multipart.{Part, FilePart}
import play.api.http.HeaderNames
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.{WSResponse, WSClient}
import util.PlayMacroLogsI
import util.ws.NingUtil.ningFut2wsScalaFut

import scala.concurrent.{ExecutionContext, Future}

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
  def mpUpload(args: IMpUploadArgs)(implicit ec: ExecutionContext, ws: WSClient): Future[WSResponse]

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

  /** Приведение ответа после аплода к внутреннему списку attachments. */
  def resp2attachments(resp: WSResponse): Seq[IPostAttachmentId]
}

/** Дефолтовая реализация multi-part upload. */
trait MpUploadSupportDflt extends IMpUploadSupport with PlayMacroLogsI {

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
  def mkRequest(args: IMpUploadArgs)(implicit ec: ExecutionContext, ws: WSClient): Future[WSResponse] = {
    LOGGER.trace("Will upload to URL: " + args.url)
    val ningClient = ws.underlying[AsyncHttpClient]
    val rb = newRequest(args, ningClient)
    args.parts
      .foreach { rb.addBodyPart }
    val req = rb.build()
    ningClient.executeRequest(req)
  }

  /** Обработать запрос, отсеивая ошибки. */
  def processResponce(args: IMpUploadArgs, resp: WSResponse)(implicit ec: ExecutionContext): Future[WSResponse] = {
    if ( isRespOk(args, resp) ) {
      Future successful resp
    } else {
      val msg = s"HTTP ${resp.status} ${resp.statusText}: ${resp.body}"
      Future failed UploadRefusedException(msg, resp)
    }
  }

  override def mpUpload(args: IMpUploadArgs)(implicit ec: ExecutionContext, ws: WSClient): Future[WSResponse] = {
    try {
      val reqFut = mkRequest(args)
      reqFut flatMap { resp =>
        processResponce(args, resp)
      }
    } catch {
      case ex: Throwable =>
        Future failed ex
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
case class UploadRefusedException(msg: String, wsResp: WSResponse) extends RuntimeException(msg)

