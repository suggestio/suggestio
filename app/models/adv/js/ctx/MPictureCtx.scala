package models.adv.js.ctx

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 14:25
 * Description: Модель контекстов данных по картинке.
 */

object MPictureCtx {

  val SAVED_FN    = "saved"
  val UPLOAD_FN   = "upload"
  val SIZE_FN     = "size"

  /** mapper из JSON. */
  implicit def reads: Reads[MPictureCtx] = (
    (__ \ SIZE_FN).readNullable[PictureSizeCtx] and
    (__ \ UPLOAD_FN).readNullable[PictureUploadCtx] and
    (__ \ SAVED_FN).readNullable[String]
  )(apply _)

  /** unmapper в JSON. */
  implicit def writes: Writes[MPictureCtx] = (
    (__ \ SIZE_FN).writeNullable[PictureSizeCtx] and
    (__ \ UPLOAD_FN).writeNullable[PictureUploadCtx] and
    (__ \ SAVED_FN).writeNullable[String]
  )(unlift(unapply))

}


/** Представление picture-контекста через распарсенный case class. */
case class MPictureCtx(
  size    : Option[PictureSizeCtx]      = None,
  upload  : Option[PictureUploadCtx]   = None,
  saved   : Option[String]              = None
)

