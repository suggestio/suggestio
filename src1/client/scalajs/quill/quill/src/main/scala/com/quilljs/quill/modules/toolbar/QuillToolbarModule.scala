package com.quilljs.quill.modules.toolbar

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 16:54
  * Description: API for toolbar settings.
  */
@js.native
trait QuillToolbarModule extends js.Object {
  // TODO Надо бы это сделать ScalaJSDefined, но quill ломается о всякие sjs$$_1$ скрытые поля, которые генерит sjs-компилятор.

  var container: js.UndefOr[js.Array[js.Any]] = js.undefined

  var handlers: js.UndefOr[js.Dictionary[js.Any]] = js.undefined

}
