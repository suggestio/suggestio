package io.suggest.file.upload

import io.suggest.file.upload.m.MFupRoot
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:50
  * Description: Circuit для формы заливки файла.
  */
class FileUploadCircuit extends CircuitLog[MFupRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.FORM_ERROR

  override protected def initialModel: MFupRoot = {
    MFupRoot(
    )
  }

  override protected def actionHandler: HandlerFunction = {
    ???
  }

}
