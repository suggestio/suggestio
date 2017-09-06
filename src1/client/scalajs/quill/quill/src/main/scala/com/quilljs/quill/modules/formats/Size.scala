package com.quilljs.quill.modules.formats

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 13:12
  * Description: Size format API.
  */

object Size {

  final val SIZE = "size"

}

@js.native
trait SizeClass extends IQuillFormat {
  var whitelist: js.Array[String] = js.native
}

@js.native
trait SizeStyle extends IQuillFormat {
  var whitelist: js.Array[String] = js.native
}


/** Size toobar btn json. */
trait SizeTb extends js.Object {

  @JSName( Size.SIZE )
  val size: js.Array[String]

}

