package com.quilljs.quill.models

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 17:07
  * Description: Interface for range selection.
  */
trait SelectionRange extends js.Object {

  val index: Int

  val length: Int

}
