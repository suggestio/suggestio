package util.img

import org.joda.time.{Instant, ReadableInstant}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{RequestHeader, Call}
import util.DateTimeUtil
import play.api.http.HeaderNames._
import io.suggest.img.ImgConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 18:57
 * Description: Вынос из контроллера ctl.Img.
 */
object ImgCtlUtil {

  /** Выдать json ошибку по поводу картинки. */
  def jsonImgError(msg: String) = JsObject(Seq(
    JSON_UPLOAD_STATUS -> JsString("error"),
    "msg"    -> JsString(msg) // TODO Добавить бы поддержку lang.
  ))


  /** Ответ на присланную для предобработки картинку. */
  def jsonTempOk(filename: String, imgUrl: Call) = {
    JsObject(List(
      JSON_UPLOAD_STATUS  -> JsString("ok"),
      JSON_IMG_KEY        -> JsString(filename),
      JSON_IMG_THUMB_URI  -> JsString(imgUrl.url)
    ))
  }


  /**
   * Проверить значение If-Modified-Since в реквесте.
   * true - not modified, false иначе.
   */
  def isModifiedSinceCached(modelTstampMs: ReadableInstant)(implicit request: RequestHeader): Boolean = {
    request.headers
      .get(IF_MODIFIED_SINCE)
      .fold(false)(isModifiedSinceCached(modelTstampMs, _))
  }

  def isModifiedSinceCached(modelTstampMs: ReadableInstant, ims: String): Boolean = {
    DateTimeUtil.parseRfcDate(ims)
      .exists { dt => !(modelTstampMs isAfter dt) }
  }


  /** rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце. */
  def withoutMs(timestampMs: Long): Instant = {
    val ims = timestampMs % 1000L
    new Instant(timestampMs - ims)
  }

}
