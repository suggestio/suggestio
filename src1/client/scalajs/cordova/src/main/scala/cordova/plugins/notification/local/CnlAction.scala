package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:11
  * Description: notification.actions
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications#actions]]
  */

object CnlAction {

  type CnlActionType_t <: js.Any

  /** @see [[https://github.com/katzer/cordova-plugin-local-notifications#properties-1]] */
  object Types {
    private def _input = "input"
    def `button+input` = ("button+" + _input).asInstanceOf[CnlActionType_t]
    def `input` = _input.asInstanceOf[CnlActionType_t]
  }

}


trait CnlAction extends js.Object {
  val id: String
  val title: String
  val `type`: js.UndefOr[CnlAction.CnlActionType_t] = js.undefined
  val launch: js.UndefOr[js.Any] = js.undefined
  val ui: js.UndefOr[js.Any] = js.undefined  // iOS
  val needsAuth: js.UndefOr[js.Any] = js.undefined  // iOS
  val icon: js.UndefOr[String] = js.undefined
  val emptyText: js.UndefOr[String] = js.undefined
  val submitTitle: js.UndefOr[String] = js.undefined // iOS
  val editable: js.UndefOr[Boolean] = js.undefined // android
  val choices: js.UndefOr[js.Any] = js.undefined
  val defaultValue: js.UndefOr[String] = js.undefined  // windows
}
