package util.img

import _root_.util.DateTimeUtil
import play.api.mvc.{Call, Controller}
import util.acl.IsAuth
import models.MPictureTmp
import play.api.libs.json._
import io.suggest.img.{ImgCrop, SioImageUtilT}
import play.api.Play.current
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */
object ImgFormUtil {
  import play.api.data.Forms._

  /** Маппер для поля с картинкой. */
  val pictureM  = nonEmptyText(minLength=5, maxLength=64)
    .verifying("Invalid image id.", MPictureTmp.isKeyValid(_))
    .verifying("Expired image id.", MPictureTmp.find(_).isDefined)


  /** Сколько времени кешировать temp-картинки на клиенте. */
  val TEMP_IMG_CACHE_SECONDS = {
    val cacheMinutes = current.configuration.getInt("img.temp.cache.client.minutes") getOrElse 30
    val cacheDuration = cacheMinutes.minutes
    cacheDuration.toSeconds.toInt
  }

  /** Маппинг обязательного параметра кропа на реальность. */
  val imgCropM = nonEmptyText(maxLength = 16)
    .verifying("crop.invalid", ImgCrop.isValidCropStr(_))
    .transform(ImgCrop(_), {ic: ImgCrop => ic.toCropStr})

  val imgCropOptM = optional(imgCropM)

}

import ImgFormUtil._

trait TempImgActions extends Controller {

  protected def imgUtil: SioImageUtilT
  protected def reverseGetTempImg(key: String): Call
  // TODO Надо абстрагировать ActionBuilder[R[_]] от экшенов. Для этого надо сначала [[https://github.com/playframework/playframework/pull/1844]]

  /** Загрузка сырой картинки для дальнейшей базовой обработки. */
  def handleTempImg = IsAuth(parse.multipartFormData) { implicit request =>
    request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        val mptmp = MPictureTmp.getForTempFile(fileRef)
        try {
          imgUtil.convert(srcFile, mptmp.file)
        } finally {
          srcFile.delete()
        }
        val reply = JsObject(List(
          "status"     -> JsString("ok"),
          "image_key"  -> JsString(mptmp.key),
          "image_link" -> JsString(reverseGetTempImg(mptmp.key).url)
        ))
        Ok(reply)

      case None =>
        BadRequest("Picture blob not found in request.")
    }
  }


  /** Раздавалка картинок, созданных в [[handleTempImg]]. */
  def getTempImg(key: String) = IsAuth { implicit request =>
    MPictureTmp.find(key) match {
      case Some(mptmp) =>
        val f = mptmp.file
        Ok.sendFile(f, inline=true)
          .withHeaders(
            LAST_MODIFIED -> DateTimeUtil.df.print(f.lastModified),
            CACHE_CONTROL -> ("public, max-age=" + TEMP_IMG_CACHE_SECONDS)
          )

      case None => NotFound("No such image.")
    }
  }

}

