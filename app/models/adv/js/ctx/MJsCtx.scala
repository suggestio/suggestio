package models.adv.js.ctx

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 17:45
 * Description: Модель-враппер над json для быстрого прозрачного доступа к служебным данным.
 */
object MJsCtx {

  val PICTURE_FN  = "_picture"
  val TARGET_FN   = "_target"

  implicit def mJsCtxReads: Reads[MJsCtx] = {
    Reads {
      case jso: JsObject  => JsSuccess(MJsCtx(jso))
      case _              => JsError("No JSON found")
    }
  }

}



case class MJsCtx(json: JsCtx_t) {
  import MJsCtx._

  /** top-level-поле _picture содержит инфу разную по картинке. */
  lazy val picture = {
    json \ PICTURE_FN match {
      case jso: JsObject  => Some(MPictureCtx(jso))
      case _              => None
    }
  }

  def pictureUpload = picture.flatMap(_.upload)

}


object MPictureCtx {

  val SAVED_FN    = "saved"
  val UPLOAD_FN   = "upload"
  val SIZE_FN     = "size"

}

/**
 * Модель-враппер над содержимым поля picture.
 * @param pctx ctx._picture
 */
case class MPictureCtx(pctx: JsObject) {
  import MPictureCtx._

  /** Данные по картинке, если есть. */
  lazy val size = (pctx \ SIZE_FN).asOpt[PictureSizeCtx]

  /** Параметры аплоада картинки, если есть. */
  lazy val upload = PictureUploadCtx.maybeFromJson(pctx \ UPLOAD_FN)

  /** Данные по сохранённой картинке. */
  lazy val saved: Option[String] = (pctx \ SAVED_FN).asOpt[String]

}
