package com.quilljs.delta

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:16
  * Description: Embeds.
  */

/** Ephemeral marker interface for any quill-delta embed. */
@js.native
trait IEmbed extends js.Object


/** A video embed. */
@js.native
trait VideoEmbed extends IEmbed {
  var video: String
}


/** A picture embed. */
@js.native
trait ImageEmbed extends IEmbed {
  var image: String
}

/** Url embed.
  * Found in quill-delta test sources. */
@js.native
trait UrlEmbed extends IEmbed {
  var url: String
}
