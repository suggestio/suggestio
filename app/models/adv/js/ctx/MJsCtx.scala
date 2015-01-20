package models.adv.js.ctx

import io.suggest.model.EsModel.FieldsJsonAcc
import models.adv.JsExtTargetFullT
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


/** Трейт для объединения разных вариантов реализаций MJsCtx. */
trait MJsCtx {
  def picture: Option[MPictureCtx]
  def target: Option[JsExtTargetFullT]
  def json: JsCtx_t

  def pictureUpload = picture.flatMap(_.upload)
}


case class MJsCtxJson(json: JsCtx_t) extends MJsCtx {
  import MJsCtx._

  /** top-level-поле _picture содержит инфу разную по картинке. */
  lazy val picture: Option[MPictureCtx] = {
    json \ PICTURE_FN match {
      case jso: JsObject  => Some(MPictureCtx(jso))
      case _              => None
    }
  }

  lazy val target: Option[JsExtTargetFullT] = {
    None    // TODO not yet implemented
  }

}

/**
 * Полноценный контекст, удобный для редактирования через copy().
 * @param picture Картинка, если есть.
 * @param restJson Исходный json. Контекст может содержать любые данные, и они должны сохранятся между
 *                 модификациями контекста.
 */
case class MJsCtxFull(
  picture   : Option[MPictureCtx] = None,
  target    : Option[JsExtTargetFullT] = None,
  restJson  : JsCtx_t = JsObject(Nil)
) extends MJsCtx {
  import MJsCtx._

  /** Сериализовать в json только данные. */
  def jsonOnlyData: JsCtx_t = {
    var acc: FieldsJsonAcc = Nil
    if (picture.isDefined)
      acc ::= PICTURE_FN -> picture.get.json
    if (target.isDefined)
      acc ::= TARGET_FN -> target.get.toJsTargetPlayJson
    JsObject(acc)
  }

  /** Сериализовать в контекст данные, объеденив их с внешними данные контекста. */
  override def json: JsCtx_t = {
    restJson deepMerge jsonOnlyData
  }
}



object MPictureCtx {

  val SAVED_FN    = "saved"
  val UPLOAD_FN   = "upload"
  val SIZE_FN     = "size"

  def apply(json: JsObject): MPictureCtx = MPictureCtxJson(json)
}

/** Общий интерфейс динамических. */
trait MPictureCtx {
  /** Сериализованное представление этого интсанса. */
  def json    : JsObject
  /** Данные по картинке, если есть. */
  def size    : Option[PictureSizeCtx]
  /** Параметры аплоада картинки, если есть. */
  def upload  : Option[PictureUploadCtxT]
  /** Данные по сохранённой картинке. */
  def saved   : Option[String]

  /** Враппер для вызова copy(x,y,z) или иного метода в зав-ти от ситуации. */
  def copy(size: Option[PictureSizeCtx] = this.size,
            upload  : Option[PictureUploadCtxT] = this.upload,
            saved   : Option[String] = this.saved): MPictureCtxFull
}


/**
 * Модель-враппер над содержимым поля picture.
 * @param json ctx._picture
 */
case class MPictureCtxJson(json: JsObject) extends MPictureCtx {
  import MPictureCtx._

  lazy val size = (json \ SIZE_FN).asOpt[PictureSizeCtx]
  lazy val upload = PictureUploadCtx.maybeFromJson(json \ UPLOAD_FN)
  lazy val saved: Option[String] = (json \ SAVED_FN).asOpt[String]

  /** Враппер для вызова copy или иного метода. */
  override def copy(size: Option[PictureSizeCtx] = this.size,
                    upload: Option[PictureUploadCtxT] = this.upload,
                    saved: Option[String] = this.saved): MPictureCtxFull = {
    MPictureCtxFull(size, upload, saved)
  }

}


/** Представление picture-контекста через распарсенный case class. */
case class MPictureCtxFull(
  size    : Option[PictureSizeCtx]      = None,
  upload  : Option[PictureUploadCtxT]   = None,
  saved   : Option[String]              = None
) extends MPictureCtx {
  import MPictureCtx._

  override def copy(size: Option[PictureSizeCtx] = this.size,
                    upload: Option[PictureUploadCtxT] = this.upload,
                    saved: Option[String] = this.saved): MPictureCtxFull = {
    MPictureCtxFull(size, upload, saved)
  }

  /** Сериализованное представление этого интсанса. */
  override def json: JsObject = {
    var acc: FieldsJsonAcc = Nil
    if (size.isDefined)
      acc ::= SIZE_FN -> size.get.toPlayJson
    if (upload.isDefined)
      acc ::= UPLOAD_FN -> upload.get.toPlayJson
    if (saved.isDefined)
      acc ::= SAVED_FN -> JsString(saved.get)
    JsObject(acc)
  }
}

