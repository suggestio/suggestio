package com.github.zenoamaro.react.quill

import com.quilljs.delta.Delta

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 16:12
  * Description: Scala.js API facade for "unpriviledged" editor wrapper.
  */
@js.native
trait QuillUnpriveledged extends js.Object {

  def getHTML(): String = js.native

  def getLength(): Int = js.native

  def getText(): String = js.native

  def getContents(): Delta = js.native

  /** Returns the current selection range, or null if the editor is unfocused. */
  def getSelection(): js.Object = js.native

  def getBounds(): Bounds_t = js.native

}
