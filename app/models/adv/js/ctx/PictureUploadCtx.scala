package models.adv.js.ctx

import io.suggest.model.EnumMaybeWithName
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
  implicit def reads = new Reads[PictureUploadCtxT] {
    override def reads(json: JsValue): JsResult[PictureUploadCtxT] = {
      import PictureUploadModes._
      (json \ MODE_FN)
        .validate[PictureUploadMode]
        .flatMap {
          case S2s => json.validate[S2sPictureUpload]
          case Url => JsSuccess(UrlPictureUpload)
          case C2s => JsSuccess(C2sPictureUpload)
        }
    }
  }

  /** Unmapper из JSON. */
  implicit def writes = new Writes[PictureUploadCtxT] {
    override def writes(o: PictureUploadCtxT): JsValue = {
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
sealed trait PictureUploadCtxT {
  def mode: PictureUploadMode
}


/** Режим работы через ссылку. Без параметров, поэтому сингтон. */
case object UrlPictureUpload extends PictureUploadCtxT {
  override def mode = PictureUploadModes.Url
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
case class S2sPictureUpload(url: String, partName: String) extends PictureUploadCtxT {
  override def mode = PictureUploadModes.S2s
}


/** Режим работы через отправку картинки со стороны клиента. */
case object C2sPictureUpload extends PictureUploadCtxT {
  override def mode = PictureUploadModes.C2s
}


/** Значения режимов аплоада картинок. */
object PictureUploadModes extends Enumeration with EnumMaybeWithName {

  /** Экземпляр modes-модели. */
  protected sealed class Val(val jsName: String) extends super.Val(jsName)

  type PictureUploadMode = Val
  override type T = PictureUploadMode


  /** Загрузка картинки на сервис с помощью ссылки внутри текста публикуемого сообщения. */
  val Url: PictureUploadMode = new Val("url")

  /** Сервер s.io должен отправить http-запрос на сервер сервиса. */
  val S2s: PictureUploadMode = new Val("s2s")

  /** Запрос аплода картинки должен идти через браузер клиента (на стороне js). */
  val C2s: PictureUploadMode = new Val("c2s")

  def default = Url


  /** JSON mapper */
  implicit def reads: Reads[PictureUploadMode] = {
    __.read[String]
      .map(withName)
  }

  /** JSON unmapper */
  implicit def writes: Writes[PictureUploadMode] = {
    __.write[String]
      .contramap(_.jsName)
  }

}
