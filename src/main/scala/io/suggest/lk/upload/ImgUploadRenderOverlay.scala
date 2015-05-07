package io.suggest.lk.upload

import io.suggest.js.UploadConstants
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.jquery.{JQuery, jQuery, JQueryEventObject, JQueryXHR}

import scala.concurrent.Future
import scala.scalajs.js.Dictionary
import io.suggest.img.ImgConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.05.15 13:27
 * Description: Поддержка аплоада картинок с отображением результата юзеру.
 * Результат приходит от сервера в виде верстки "оверлея".
 */
trait ImgUploadRenderOverlay extends AjaxFileUpload with ISjsLogger {

  override type FileUploadRespT = Dictionary[String]

  /** Инфа по загрузке картинки приходит в json. */
  override protected def _fileUploadAjaxRespDataType = "json"

  /** Обработка полученного положительного ответа от сервера по выполненному upload'у. */
  override def _fileUploadSuccess(resp: Dictionary[String], jqXhr: JQueryXHR, input: HTMLInputElement,
                                  cont: JQuery, e: JQueryEventObject): Future[Unit] = {
    // Надо залить полученную верстку от сервера в DOM.
    val htmlOpt = resp.get(JSON_OVERLAY_HTML)

    htmlOpt match {
      case Some(htmlStr) =>
        cont.append(htmlStr)
      case None =>
        warn("No overlay html received from server!")
    }

    // Нужно скрыть кнопку аплоада, если допускается загрузка максимум одной картинки.
    if ( !input.hasAttribute(UploadConstants.ATTR_MULTI_INDEX_COUNTER) ) {
      jQuery(input)
        .parent()
        .hide()
    }

    Future successful None
  }

}
