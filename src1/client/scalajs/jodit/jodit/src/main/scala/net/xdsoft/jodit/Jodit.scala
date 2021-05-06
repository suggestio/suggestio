package net.xdsoft.jodit

import net.xdsoft.jodit.types.IJodit

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


@js.native
@JSImport("jodit", "Jodit")
class Jodit(container: String,
            opts: IConfig = js.native ) extends IJodit {
}

@js.native
@JSImport("jodit", "Jodit")
object Jodit extends js.Object {

  val MODE_WYSIWYG,
      MODE_SOURCE: EditorMode = js.native

}
