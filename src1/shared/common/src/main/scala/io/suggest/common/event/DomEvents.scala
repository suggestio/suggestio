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
{

  def MESSAGE = "message"

  def PROGRESS = "progress"

  def CLOSE = "close"

  def ERROR = "error"

}