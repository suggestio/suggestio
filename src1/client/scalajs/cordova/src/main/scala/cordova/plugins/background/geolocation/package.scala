package cordova.plugins.background

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 16:09
  */
package object geolocation {

  type LocationProvider_t <: Int

  type Accuracy_t = Double

  type AuthorizationStatus <: Int

  type LocationErrorCode <: Int

  type ServiceMode <: js.Any

  type LogLevel <: js.Any

  type LocationId_t = Int

  type EventType = String

  type TaskId = Double

  type ActivityType <: String

}
