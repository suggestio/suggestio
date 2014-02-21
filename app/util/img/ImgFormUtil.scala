package util.img

import play.api.mvc.{Call, Controller}
import util.acl.IsAuth
import models.MPictureTmp
import play.api.libs.json._
import io.suggest.img.SioImageUtilT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */
object ImgFormUtil {
  import play.api.data.Forms._


}

trait TempImgActions extends Controller {

  protected def imgUtil: SioImageUtilT
  protected def reverseGetTempPicture(key: String): Call

  /** Загрузка сырой картинки для дальнейшей базовой обработки. */
  def handleTempPicture = IsAuth(parse.multipartFormData) { implicit request =>
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
          "image_link" -> JsString(reverseGetTempPicture(mptmp.key).url)
        ))
        Ok(reply)

      case None =>
        BadRequest("Picture blob not found in request.")
    }
  }


  /** Раздавалка картинок, созданных в [[handleTempPicture()]]. */
  def getTempPicture(key: String) = IsAuth { implicit request =>
    MPictureTmp.find(key) match {
      case Some(mptmp) => Ok.sendFile(mptmp.file, inline=true)
      case None        => NotFound("No such image.")
    }
  }

}

