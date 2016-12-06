package io.suggest.sjs.common.i18n

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:09
  * Description: JS-интерфейсы для client-side словаря локализации.
  */

/** интерфейс для messages-объекта в рамках одного языка. */
@js.native
sealed trait JsMessagesSingleLang extends js.Object {

  def apply(code: String): String = js.native

  //val messages: js.Dictionary[String] = js.native

}
