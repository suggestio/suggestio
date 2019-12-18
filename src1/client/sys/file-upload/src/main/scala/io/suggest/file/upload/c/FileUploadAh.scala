package io.suggest.file.upload.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.file.upload.m.MFupRoot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:58
  * Description: Контроллер заливки файла в форме.
  */
class FileUploadAh[M](
                       modelRW: ModelRW[M, MFupRoot],
                     )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    ???
  }

}
