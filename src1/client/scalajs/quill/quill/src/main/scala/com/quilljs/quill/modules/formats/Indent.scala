package com.quilljs.quill.modules.formats

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 13:49
  * Description: Quill text indent format.
  */
object Indent {

  final val INDENT = "indent"

  def PLUS_1 = "+1"

  def MINUS_1 = "-1"

}


/** Text indent toolbar btn. */
trait IndentTb extends js.Object {
  val indent: String
}
