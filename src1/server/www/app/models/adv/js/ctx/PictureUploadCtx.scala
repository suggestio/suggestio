package models.adv.js.ctx

import io.suggest.adv.ext.model.ctx.{MPictureUploadMode, MPictureUploadModes}
import io.suggest.adv.ext.model.ctx.PicUploadCtx._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.01.15 16:59
 * Description: Представления контекста для доступа к данных по аплоаду картинки на удалённый сервис.
 * В рамках контекста есть несколько режимов работы с разными наборами данных.
 * В сериализованной форме это json, содержащий обязательное поле mode и другие возможные поля.
 */
object PictureUploadCtx {

  /** mapper из JSON. */
  implicit def reads = new Reads[PictureUploadCtx] {
    override def reads(json: JsValue): JsResult[PictureUploadCtx] = {
      import MPictureUploadModes._
      (json \ MODE_FN)
        .validate[MPictureUploadMode]
        .flatMap {
          case S2s => json.validate[S2sPictureUpload]
        }
    }
  }

  /** Unmapper из JSON. */
  implicit def writes = new Writes[PictureUploadCtx] {
    override def writes(o: PictureUploadCtx): JsValue = {
      o match {
        case s2s: S2sPictureUpload =>
          S2sPictureUpload.writes.writes(s2s)
        case _ =>
          JsObject(Seq(
            MODE_FN -> JsString(o.mode.value)
          ))
      }
    }
  }

}



/** Трейт для данных по режиму upload'а. */
sealed trait PictureUploadCtx {
  def mode: MPictureUploadMode
}


/** Статическая сторона данных по режиму s2s-upload. */
object S2sPictureUpload {

  implicit def reads: Reads[S2sPictureUpload] = (
    (__ \ URL_FN).read[String] and
    (__ \ PART_NAME_FN).read[String]
  )(apply _)

  implicit def writes = new Writes[S2sPictureUpload] {
    override def writes(o: S2sPictureUpload): JsValue = {
      JsObject(Seq(
        MODE_FN       -> JsString(o.mode.value),
        URL_FN        -> JsString(o.url),
        PART_NAME_FN  -> JsString(o.partName)
      ))
    }
  }

}

/** Модель данных по s2s upload. */
case class S2sPictureUpload(url: String, partName: String) extends PictureUploadCtx {
  override def mode = MPictureUploadModes.S2s
}
