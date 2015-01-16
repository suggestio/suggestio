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

  implicit val mJsCtxReads: Reads[MJsCtx] = {
    Reads {
      case jso: JsObject  => JsSuccess(MJsCtx(jso))
      case _              => JsError("No JSON found")
    }
  }

}


import MJsCtx._


case class MJsCtx(json: JsCtx_t) {

  /** top-level-поле _picture содержит инфу разную по картинке. */
  lazy val picture = {
    json \ PICTURE_FN match {
      case jso: JsObject  => Some(MPictureCtx(jso))
      case _              => None
    }
  }

  def pictureUpload = picture.flatMap(_.upload)

}


/**
 * Модель-враппер над содержимым поля picture.
 * @param pctx ctx._picture
 */
case class MPictureCtx(pctx: JsObject) {

  /** Данные по картинке, если есть. */
  lazy val size = (pctx \ "size").asOpt[PictureSizeCtx]

  /** Параметры аплоада картинки, если есть. */
  lazy val upload = PictureUploadCtx.maybeFromJson(pctx \ "upload")

  /** Данные по сохранённой картинке. */
  lazy val saved: Option[String] = (pctx \ "saved").asOpt[String]

}
