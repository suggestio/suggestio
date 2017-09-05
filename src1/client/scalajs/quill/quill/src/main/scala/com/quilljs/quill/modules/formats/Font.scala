package com.quilljs.quill.modules.formats

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.09.17 22:30
  * Description: Quill font format.
  */
object Font {

  final val FONT = "font"

}


@js.native
trait Font extends IQuillFormat {

  var whitelist: js.Array[String] = js.native

}


/** Запись о кнопке выбора шрифта в спеке toolbar'а. */
trait FontTb extends js.Object {

  @JSName( Font.FONT )
  val font: js.Any

}
