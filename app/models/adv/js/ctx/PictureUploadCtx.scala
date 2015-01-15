package models.adv.js.ctx

import io.suggest.model.EnumMaybeWithName
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.01.15 16:59
 * Description: Представления контекста для доступа к данных по аплоаду картинки на удалённый сервис.
 * В рамках контекста есть несколько режимов работы с разными наборами данных.
 * В сериализованной форме это json, содержащий обязательное поле mode и другие возможные поля.
 */
object PictureUploadCtx {

  def MODE_FN = "mode"

  def maybeFromJson(json1: JsValue): Option[PictureUploadCtxT] = {
    (json1 \ MODE_FN)
      .asOpt[String]
      .flatMap { PictureUploadModes.maybeWithName }
      .map { _.fromJson(json1) }
  }

  def fromJson(json1: JsValue): PictureUploadCtxT = {
    maybeFromJson(json1) getOrElse {
      PictureUploadModes.default.fromJson(json1)
    }
  }

}


import PictureUploadCtx._


/** Трейт для данных по режиму upload'а. */
sealed trait PictureUploadCtxT {

  def mode: PictureUploadMode

  def toPlayJson: JsObject = JsObject(toPlayJsonFields)

  def toPlayJsonFields: FieldsJsonAcc = {
    List(
      MODE_FN -> JsString(mode.jsName)
    )
  }

}

case object SkipPictureUpload extends PictureUploadCtxT {
  override def mode = PictureUploadModes.Skip
}

/** Режим работы через ссылку. Без параметров, поэтому сингтон. */
case object UrlPictureUpload extends PictureUploadCtxT {
  override def mode = PictureUploadModes.Url
}


/** Статическая сторона данных по режиму s2s-upload. */
object S2sPictureUpload {
  def URL_FN = "url"
  def PART_NAME_FN = "partName"

  def apply(json1: JsValue): S2sPictureUpload = {
    val resOpt = for {
      url <- (json1 \ URL_FN).asOpt[String]
      partName <- (json1 \ PART_NAME_FN).asOpt[String]
    } yield {
      S2sPictureUpload(url = url, partName = partName)
    }
    resOpt.get
  }
}

/** Модель данных по s2s upload. */
case class S2sPictureUpload(url: String, partName: String) extends PictureUploadCtxT {
  import S2sPictureUpload._

  override def mode = PictureUploadModes.S2s

  override def toPlayJsonFields: FieldsJsonAcc = {
    URL_FN -> JsString(url) ::
      PART_NAME_FN -> JsString(partName) ::
      super.toPlayJsonFields
  }
}


/** Режим работы через отправку картинки со стороны клиента. */
case object C2sPictureUpload extends PictureUploadCtxT {
  override def mode = PictureUploadModes.C2s
}


/** Значения режимов аплоада картинок. */
object PictureUploadModes extends Enumeration with EnumMaybeWithName {

  /** Экземпляр modes-модели. */
  abstract protected sealed class Val(val jsName: String) extends super.Val(jsName) {
    /** Тип, содержащий данные указанного режима. */
    type X <: PictureUploadCtxT
    def fromJson(json1: JsValue): X
  }

  type PictureUploadMode = Val
  override type T = PictureUploadMode


  /** Загрузка картинки на сервис с помощью ссылки внутри текста публикуемого сообщения. */
  val Url: PictureUploadMode = new Val("url") {
    override type X = UrlPictureUpload.type
    override def fromJson(json1: JsValue) = UrlPictureUpload
  }

  /** Сервер s.io должен отправить http-запрос на сервер сервиса. */
  val S2s: PictureUploadMode = new Val("s2s") {
    override type X = S2sPictureUpload
    override def fromJson(json1: JsValue) = S2sPictureUpload(json1)
  }

  /** Запрос аплода картинки должен идти через браузер клиента (на стороне js). */
  val C2s: PictureUploadMode = new Val("c2s") {
    override type X = C2sPictureUpload.type
    override def fromJson(json1: JsValue) = C2sPictureUpload
  }

  /** js-сторона предлагает серверу не париться насчет загрузки картинки. */
  val Skip: PictureUploadMode = new Val("skip") {
    override type X = SkipPictureUpload.type
    override def fromJson(json1: JsValue) = SkipPictureUpload
  }

  def default = Url

}
