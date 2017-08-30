package com.quilljs.quill.events

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 17:24
  * Description: API facades/stubs for Quill editor events.
  */
@js.native
sealed trait IEvent extends js.Object


@js.native
sealed trait KeyPressEvent extends IEvent

@js.native
sealed trait KeyDownEvent extends IEvent

@js.native
sealed trait KeyUpEvent extends IEvent
