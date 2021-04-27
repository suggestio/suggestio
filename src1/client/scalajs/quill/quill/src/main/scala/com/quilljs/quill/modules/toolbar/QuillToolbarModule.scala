package com.quilljs.quill.modules.toolbar

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 16:54
  * Description: API for toolbar settings.
  */
trait QuillToolbarModule extends js.Object {

  val container: js.UndefOr[js.Array[js.Any]] = js.undefined

  val handlers: js.UndefOr[js.Dictionary[js.Any]] = js.undefined

}
