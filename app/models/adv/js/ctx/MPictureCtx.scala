package models.adv.js.ctx

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.adv.ext.model.ctx.MAdPictureCtx._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 14:25
 * Description: Модель контекстов данных по картинке.
 */

object MPictureCtx {

  /** mapper из JSON. */
  implicit def reads: Reads[MPictureCtx] = (
    (__ \ SIZE_FN).readNullable[String] and
    (__ \ UPLOAD_FN).readNullable[PictureUploadCtx] and
    (__ \ SIO_URL_FN).readNullable[String] and
    (__ \ SAVED_FN).readNullable[String]
  )(apply _)

  /** unmapper в JSON. */
  implicit def writes: Writes[MPictureCtx] = (
    (__ \ SIZE_FN).writeNullable[String] and
    (__ \ UPLOAD_FN).writeNullable[PictureUploadCtx] and
    (__ \ SIO_URL_FN).writeNullable[String] and
    (__ \ SAVED_FN).writeNullable[String]
  )(unlift(unapply))

}


/** Представление picture-контекста через распарсенный case class. */
case class MPictureCtx(
  size    : Option[String]              = None,
  upload  : Option[PictureUploadCtx]    = None,
  sioUrl  : Option[String]              = None,
  saved   : Option[String]              = None
)

