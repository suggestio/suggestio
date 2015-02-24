package models.adv.js.ctx

import io.suggest.model.{EnumJsonReadsT, EnumMaybeWithName}
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

  val MODE_FN = "mode"

  /** mapper из JSON. */
  implicit def reads = new Reads[PictureUploadCtx] {
    override def reads(json: JsValue): JsResult[PictureUploadCtx] = {
      import PictureUploadModes._
      (json \ MODE_FN)
        .validate[PictureUploadMode]
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
            MODE_FN -> JsString(o.mode.jsName)
          ))
      }
    }
  }

}



/** Трейт для данных по режиму upload'а. */
sealed trait PictureUploadCtx {
  def mode: PictureUploadMode
}


/** Статическая сторона данных по режиму s2s-upload. */
object S2sPictureUpload {
  import PictureUploadCtx.MODE_FN

  val URL_FN = "url"
  val PART_NAME_FN = "partName"

  implicit def reads: Reads[S2sPictureUpload] = (
    (__ \ URL_FN).read[String] and
    (__ \ PART_NAME_FN).read[String]
  )(apply _)

  implicit def writes = new Writes[S2sPictureUpload] {
    override def writes(o: S2sPictureUpload): JsValue = {
      JsObject(Seq(
        MODE_FN       -> JsString(o.mode.jsName),
        URL_FN        -> JsString(o.url),
        PART_NAME_FN  -> JsString(o.partName)
      ))
    }
  }

}

/** Модель данных по s2s upload. */
case class S2sPictureUpload(url: String, partName: String) extends PictureUploadCtx {
  override def mode = PictureUploadModes.S2s
}


/** Значения режимов аплоада картинок. */
// TODO Выпилить эту модель наверное надо?
object PictureUploadModes extends Enumeration with EnumMaybeWithName with EnumJsonReadsT {

  /** Экземпляр modes-модели. */
  protected sealed class Val(val jsName: String) extends super.Val(jsName)

  type PictureUploadMode = Val
  override type T = PictureUploadMode

  /** Сервер s.io должен отправить http-запрос на сервер сервиса. */
  val S2s: PictureUploadMode = new Val("s2s")

  /** JSON unmapper */
  implicit def writes: Writes[PictureUploadMode] = {
    __.write[String]
      .contramap(_.jsName)
  }

}
