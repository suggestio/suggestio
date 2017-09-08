package com.quilljs.delta

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:16
  * Description: Delta Embed model.
  */

/** Ephemeral marker interface for any quill-delta embed. */
@js.native
sealed trait DeltaEmbed extends js.Object {

  var video: UndefOr[String] = js.native

  var image: UndefOr[String] = js.native

}
