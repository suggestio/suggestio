package util.img

import java.time.Instant

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import javax.inject.Inject

import io.suggest.dt.DateTimeUtil
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{Call, RequestHeader}
import play.twirl.api.Html
import util.HtmlCompressUtil
import play.api.http.HeaderNames._
import io.suggest.img.ImgConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 18:57
 * Description: Вынос из контроллера ctl.Img.
 */
class ImgCtlUtil @Inject() (htmlCompressUtil: HtmlCompressUtil) {

  /** Выдать json ошибку по поводу картинки. */
  def jsonImgError(msg: String) = JsObject(Seq(
    JSON_UPLOAD_STATUS -> JsString("error"),
    "msg"    -> JsString(msg) // TODO Добавить бы поддержку lang.
  ))


  /** Ответ на присланную для предобработки картинку. */
  def jsonTempOk(filename: String, imgUrl: Call, ovlOpt: Option[Html] = None) = {
    var acc: FieldsJsonAcc = List(
      JSON_UPLOAD_STATUS  -> JsString("ok"),
      JSON_IMG_KEY        -> JsString(filename),
      JSON_IMG_THUMB_URI  -> JsString(imgUrl.url)
    )
    if (ovlOpt.isDefined)
      acc ::= JSON_OVERLAY_HTML -> htmlCompressUtil.html2jsStr(ovlOpt.get)
    JsObject(acc)
  }


  /**
   * Проверить значение If-Modified-Since в реквесте.
   * true - not modified, false иначе.
   */
  def isModifiedSinceCached(modelTstampMs: Instant)(implicit request: RequestHeader): Boolean = {
    request.headers
      .get(IF_MODIFIED_SINCE)
      .fold(false)(isModifiedSinceCached(modelTstampMs, _))
  }

  def isModifiedSinceCached(modelTstampMs: Instant, ims: String): Boolean = {
    DateTimeUtil.parseRfcDate(ims)
      .exists { dt => !modelTstampMs.isAfter(dt.toInstant) }
  }


  /** rfc date не содержит миллисекунд. Нужно округлять таймштамп, чтобы был 000 в конце. */
  def withoutMs(timestampMs: Long): Instant = {
    Instant.ofEpochSecond(timestampMs / 1000)
  }

}
