package io.suggest.lk.upload

import io.suggest.js.UploadConstants
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.log.ILog
import io.suggest.sjs.common.util.IContainers
import org.scalajs.dom.File
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.jquery._

import scala.concurrent.Future
import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 16:03
 * Description: Поддержка реагирования на инпуты, помеченные как js-file-upload.
 */

/** Интерфейс для передачи полученного файла. */
trait IHandleFile4Upload {
  protected def _handleFile4Upload(file: File, input: HTMLInputElement, cont: JQuery, e: JQueryEventObject): Future[_]
}


trait InputFileUploadOnChange extends IContainers with ILog with IHandleFile4Upload {

  /** Вызов инициализации события change для аплоада. */
  protected def initJsFileUploadOnChange(): Unit = {
    _imgInputContainers.foreach { cont =>
      cont.on("change", "." + UploadConstants.JS_FILE_UPLOAD_CLASS, {
        (input: HTMLInputElement, e: JQueryEventObject) =>
          if (input.`type` == "file") {
            e.preventDefault()
            val files = input.files
            if (files.length > 0) {
              val file = files(0)
              _handleFile4Upload(file, input, cont, e)
            } else {
              LOG.log(msg = "No file selected for upload")
            }

          } else {
            LOG.warn("Invalid input tag binded for upload event: name=" + input.name + " " + input.value)
          }
      }: ThisFunction)
    }
  }

}


/** Подцепить инициализацию аплоада к init(). */
trait InitInputFileUploadOnChange extends InputFileUploadOnChange with IInit {
  abstract override def init(): Unit = {
    super.init()
    initJsFileUploadOnChange()
  }
}

