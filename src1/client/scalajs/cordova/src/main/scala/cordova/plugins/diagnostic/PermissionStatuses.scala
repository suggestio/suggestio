package cordova.plugins.diagnostic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 16:31
  * Description: Permission statuses constants.
  */
@js.native
trait PermissionStatuses extends js.Object {

  // all platforms
  val NOT_REQUESTED, GRANTED, DENIED: PermissionStatus_t = js.native

  // android
  val DENIED_ALWAYS: js.UndefOr[PermissionStatus_t] = js.native

  // ios
  val GRANTED_WHEN_IN_USE, RESTRICTED: js.UndefOr[PermissionStatus_t] = js.undefined

}
