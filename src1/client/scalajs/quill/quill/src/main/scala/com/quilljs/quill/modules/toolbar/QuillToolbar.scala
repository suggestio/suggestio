package com.quilljs.quill.modules.toolbar

import com.quilljs.quill.QuillStatic
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 17:01
  * Description: Interface for quill toolbar instance.
  */
@js.native
trait QuillToolbar extends js.Object {

  val container: HTMLElement = js.native

  val quill: QuillStatic = js.native

}
