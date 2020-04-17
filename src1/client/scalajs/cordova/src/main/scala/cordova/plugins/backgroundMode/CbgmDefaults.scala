package cordova.plugins.backgroundMode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.04.2020 18:07
  * Description: setDefaults() | configure() arg$1 object API.
  *
  * The title, text and icon for that notification can be customized as below.
  * Also, by default the app will come to foreground when tapping on the notification.
  * That can be changed by setting resume to false.
  *
  * On Android 5.0+, the color option will set the background color of the notification circle.
  * Also on Android 5.0+, setting hidden to false will make the notification visible on lockscreen.
  *
  */
trait CbgmDefaults extends js.Object {

  val title: js.UndefOr[String] = js.undefined
  val text: js.UndefOr[String] = js.undefined

  /** this will look for icon.png in platforms/android/res/drawable|mipmap */
  val icon: js.UndefOr[String] = js.undefined

  /** hex format like 'F14F4D' */
  val color: js.UndefOr[String] = js.undefined

  val resume: js.UndefOr[Boolean] = js.undefined
  val hidden: js.UndefOr[Boolean] = js.undefined
  val bigText: js.UndefOr[Boolean] = js.undefined

  /** In silent mode the plugin will not display a notification - which is not the default.
    * Be aware that Android recommends adding a notification otherwise the OS may pause the app. */
  val silent: js.UndefOr[Boolean] = js.undefined

}
