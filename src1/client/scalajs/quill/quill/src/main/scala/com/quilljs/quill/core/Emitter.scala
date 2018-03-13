package com.quilljs.quill.core

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 17:54
  * Description: API interface for quill event emitter.
  */
@js.native
@JSImport("quill/core/emitter", JSImport.Default)
object Emitter extends js.Object {

  val sources: EmitterSources = js.native

  val events: EmitterEvents = js.native

}


@js.native
trait EmitterSources extends js.Object {

  val API: String = js.native
  val SILENT: String = js.native
  val USER: String = js.native

}


@js.native
trait EmitterEvents extends js.Object {

  val EDITOR_CHANGE: String = js.native
  val SCROLL_BEFORE_UPDATE: String = js.native
  val SCROLL_OPTIMIZE: String = js.native
  val SCROLL_UPDATE: String = js.native
  val SELECTION_CHANGE: String = js.native
  val TEXT_CHANGE: String = js.native

}