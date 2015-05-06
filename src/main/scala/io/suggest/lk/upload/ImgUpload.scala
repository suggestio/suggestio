package io.suggest.lk.upload

import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.jquery.{JQuery, JQueryEventObject, JQueryXHR}

import scala.concurrent.Future
import scala.scalajs.js.Dictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.05.15 13:27
 * Description: Поддержка аплоада картинок с отображением результата юзеру.
 * Результат обычно содержит превью загруженной картинки.
 */
trait ImgUpload extends JsFileUploadOnChange {

  override type FileUploadRespT = Dictionary[String]

  /** Инфа по загрузке картинки приходит в json. */
  override protected def _fileUploadAjaxRespDataType = "json"

  /** Обработка полученного положительного ответа от сервера по выполненному upload'у. */
  override def _fileUploadSuccess(resp: Dictionary[String], jqXhr: JQueryXHR, input: HTMLInputElement,
                                  cont: JQuery, e: JQueryEventObject): Future[Unit] = {
    // Надо залить полученную верстку от сервера в DOM.

    ???
  }

}
