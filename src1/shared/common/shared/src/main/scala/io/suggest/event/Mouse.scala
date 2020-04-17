package io.suggest.event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:17
  * Description: Mouse DOM events constants.
  */
trait Mouse {

  def MOUSE_DOWN      = "mousedown"
  def MOUSE_MOVE      = "mousemove"
  def MOUSE_UP        = "mouseup"

}
object MouseEvents extends Mouse with Click