package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 22:02
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation#task-event]]
  */
trait HeadlessTaskEvent extends js.Object {

  val name: HeadlessTaskEvent.Name

  val params: js.UndefOr[js.Object] = js.undefined

}


object HeadlessTaskEvent {

  type Name <: String

  object Name {
    final def LOCATION = "location".asInstanceOf[Name]
    final def STATIONARY = "stationary".asInstanceOf[Name]
    final def ACTIVITY = "activity".asInstanceOf[Name]
  }

}
