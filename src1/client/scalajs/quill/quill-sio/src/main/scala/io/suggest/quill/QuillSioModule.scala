package io.suggest.quill

import com.softwaremill.macwire._
import io.suggest.quill.u.{QuillDeltaJsUtil, QuillInit}
import io.suggest.quill.v.{QuillCss, QuillEditorR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 11:01
  * Description: macwire compile-time DI module for quill-sio subproject.
  */
class QuillSioModule {

  lazy val quillEditorR = wire[QuillEditorR]

  lazy val quillDeltaJsUtil = wire[QuillDeltaJsUtil]

  lazy val quillInit = wire[QuillInit]

  def quillCss = wire[QuillCss]

}
