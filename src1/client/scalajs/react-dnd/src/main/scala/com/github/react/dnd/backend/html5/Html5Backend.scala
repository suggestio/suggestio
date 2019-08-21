package com.github.react.dnd.backend.html5

import com.github.react.dnd.{DropAccept_t, IDndBackend}
import org.scalajs.dom.html.Image

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2019 12:18
  */
@js.native
@JSImport(PACKAGE_NAME, JSImport.Default)
object Html5Backend extends IDndBackend


/** A function returning a transparent empty Image.
  * Use connect.dragPreview() of the DragSourceConnector to hide the browser-drawn drag preview completely.
  * Handy for drawing the custom drag layers with DragLayer. Note that the custom drag previews don't work in IE.
  */
@js.native
@JSImport(PACKAGE_NAME, "getEmptyImage")
object getEmptyImage extends js.Function0[Image] {
  override def apply(): Image = js.native
}


/** An enumeration of three constants.
  * May be registered as the drop targets for these types to handle the drop of the native files, URLs, or the regular page text.
  */
@js.native
@JSImport(PACKAGE_NAME, "NativeTypes")
object NativeTypes extends js.Object {

  /** - getItem().files, with an array of files
    * - getItem().items, with event.dataTransfer.items (which you can use to list files when a directory is dropped)
    */
  val FILE: DropAccept_t = js.native

  /** - getItem().urls, an array with the dropped URLs. */
  val URL: DropAccept_t = js.native

  /** - getItem().text, the text that has been dropped. */
  val TEXT: DropAccept_t = js.native

}
