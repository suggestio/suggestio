package io.suggest.common.event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 21:19
  * Description: События DOM.
  */
object DomEvents
  extends Click
  with Drag
  with Mouse
  with Move
  with Touch
  with VisibilityChangeEvents
{

  def CHANGE = "change"

  def MESSAGE = "message"
  def FETCH = "fetch"

  def PROGRESS = "progress"

  def OPEN  = "open"
  def CLOSE = "close"

  def ERROR = "error"

  private def BEFORE = "before"
  private def UNLOAD = "unload"

  def BEFORE_UNLOAD = BEFORE + UNLOAD

  def SCROLL = "scroll"

}
