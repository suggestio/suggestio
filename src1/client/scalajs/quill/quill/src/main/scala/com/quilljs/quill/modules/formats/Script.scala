package com.quilljs.quill.modules.formats

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 13:27
  * Description: Script JSONs.
  */
object Script {

  final val SCRIPT = "script"

  def SUPER = "super"

  def SUB = "sub"

}


trait ScriptTb extends js.Object {

  @JSName( Script.SCRIPT )
  val script: String

}
