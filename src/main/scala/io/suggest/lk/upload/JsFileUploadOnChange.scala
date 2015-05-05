package io.suggest.lk.upload

import io.suggest.js.UploadConstants
import io.suggest.sjs.common.util.IContainers
import org.scalajs.jquery.JQueryEventObject

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 16:03
 * Description: Поддержка реагирования на инпуты, помеченные как js-file-upload.
 */
trait JsFileUploadOnChange extends IContainers {

  /** Вызов инициализации события change для аплоада. */
  protected def initJsFileUploadOnChange(): Unit = {
    _containers.foreach { cont =>
      cont.on("change", "." + UploadConstants.JS_FILE_UPLOAD_CLASS, { e: JQueryEventObject =>
        e.preventDefault()
        if (e.currentTarget)
      })
    }
  }

}
