package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:45
  * Description: Standard events.
  */
object CnlEvents {

  type CnlEvent_t <: js.Any

  def add = "add"
  def trigger = "trigger"
  def click = "click"
  def clear = "clear"
  def cancel = "cancel"
  def update = "update"
  def clearAll = "clearall"
  def cancelAll = "cancelAll"

}


trait CnlEventOpts extends js.Object

