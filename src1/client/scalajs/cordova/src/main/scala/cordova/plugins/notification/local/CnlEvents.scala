package cordova.plugins.notification.local

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:45
  * Description: Standard events.
  */
object CnlEvents {

  final def ADD = "add"
  final def TRIGGER = "trigger"
  final def CLICK = "click"
  final def CLEAR = "clear"
  final def CANCEL = "cancel"
  final def UPDATE = "update"
  final def CLEAR_ALL = "clearall"
  final def CANCEL_ALL = "cancelall"

  // Помимо этих событий, могут быть ещё произвольные, заданные через notification.action[].id: String

}
