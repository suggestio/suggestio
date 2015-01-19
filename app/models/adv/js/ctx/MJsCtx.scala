package models.adv.js.ctx

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 17:45
 * Description: Модель-враппер над json для быстрого прозрачного доступа к служебным данным.
 * Динамические части моделей существуют как в виде json-обёрток, так и в виде нормальных моделей.
 * Это сделано, т.к. далеко не всегда нужно что-то парсить и менять в контексте, а полный парсинг контекста
 * в будущем может стать ресурсоёмким процессом.
 */
object MJsCtx {

  val PICTURE_FN  = "_picture"
  val TARGET_FN   = "_target"

  implicit def mJsCtxReads: Reads[MJsCtx] = {
    Reads {
      case jso: JsObject  => JsSuccess( apply(jso) )
      case _              => JsError("No JSON found")
    }
  }

  def apply(json: JsCtx_t): MJsCtx = MJsCtxJson(json)

}


/** Трейт для объединения разных вариантов. */
trait MJsCtx {
  def picture: Option[MPictureCtx]
  def json: JsCtx_t

  def pictureUpload = picture.flatMap(_.upload)
}


case class MJsCtxJson(json: JsCtx_t) extends MJsCtx {
  import MJsCtx._

  /** top-level-поле _picture содержит инфу разную по картинке. */
  lazy val picture = {
    json \ PICTURE_FN match {
      case jso: JsObject  => Some(MPictureCtx(jso))
      case _              => None
    }
  }

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
