package models.mext

import java.io.ByteArrayOutputStream

import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import play.api.http.HeaderNames
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.{WSResponse, WSRequestHolder, WSClient}

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

}

/** Дефолтовая реализация multi-part upload. */
trait MpUploadSupportDflt extends IMpUploadSupport {

  /**
   * Узнать URL для запроса. Аргументы содержат только опциональный URL, если он динамический.
   * @param args Аргументы upload.
   * @return
   */
  def getUploadUrl(args: IMpUploadArgs): String

  /**
   * Сборка тела multi-part POST.
   * @param args Аргументы [[IMpUploadArgs]].
   * @param boundary Используемая граница между частями. Она указывается в content-type при запросе.
   * @return Байты тела запроса.
   */
  def mkMpPostBody(args: IMpUploadArgs, boundary: String): Array[Byte] = {
    // Собрать POST-запрос и запустить его на исполнение
    val entity = MultipartEntityBuilder.create()
      .setBoundary(boundary)
    val nearLen = args.parts.foldLeft(0) { (l0, part) =>
      val partCt = ContentType.create(part.ct)
      val d = part.data
      val partBody = new ByteArrayBody(d, partCt, part.fileName)
      entity.addPart(part.name, partBody)
      l0 + d.length + 255
    }
    val baos = new ByteArrayOutputStream(nearLen)
    val resp = entity.build()
    resp.writeTo(baos)
    baos.toByteArray
  }

  /** Генерация boundary. */
  def boundary(args: IMpUploadArgs): String = {
    "-----BOUNDARY-" + args.hashCode() + "--" + (System.currentTimeMillis() / 1000L) + "-----"
  }

  /** Создание экземпляра нового реквеста. */
  def newRequest(args: IMpUploadArgs)(implicit ws: WSClient): WSRequestHolder = {
    ws.url( getUploadUrl(args) )
  }

  /** Является ли ответ по запросу правильным. false - если ошибка. */
  def isRespOk(args: IMpUploadArgs, resp: WSResponse): Boolean

  /** Запуск HTTP-запроса. */
  def mkRequest(args: IMpUploadArgs)(implicit ec: ExecutionContext, ws: WSClient): Future[WSResponse] = {
    val _boundary = boundary(args)
    newRequest(args)
      .withHeaders(
        HeaderNames.CONTENT_TYPE -> ("multipart/form-data; boundary=" + _boundary)
      )
      .post(mkMpPostBody(args, _boundary))
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
  def parts     : TraversableOnce[IUploadPart]
  /** Ссылка для аплоада, если динамическая. */
  def url       : Option[String]
  /** access token, если oauth1 сервис. Иначе None. */
  def oa1AcTok  : Option[RequestToken]
}

/** Дефолтовая реализацяи [[IMpUploadArgs]]. */
case class MpUploadArgs(
  override val parts    : TraversableOnce[IUploadPart],
  override val url      : Option[String],
  override val oa1AcTok : Option[RequestToken] = None
)
  extends IMpUploadArgs


/** Сервис отказал в аплоаде. */
case class UploadRefusedException(msg: String, wsResp: WSResponse) extends RuntimeException(msg)

/** Интерфейс описания одной части для multipart-upload'a. */
trait IUploadPart {
  /** Тело части. */
  def data: Array[Byte]
  /** Название части. */
  def name: String
  /** content-type. */
  def ct: String
  /** Имя файла. */
  def fileName: String
}

/** Дефолтовая реализация [[IUploadPart]]. */
case class UploadPart(
  override val data: Array[Byte],
  override val name: String,
  override val ct: String,
  override val fileName: String
)
  extends IUploadPart
