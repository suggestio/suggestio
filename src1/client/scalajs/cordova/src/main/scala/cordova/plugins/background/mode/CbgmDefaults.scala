package cordova.plugins.background.mode

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


  // ------------------------------ TheBosZ/cordova-plugin-run-in-background -----------------------------------

  /** Shown when the user views the app's notification settings. */
  val channelName: js.UndefOr[String] = js.undefined
  /** Shown when the user views the channel's settings. */
  val channelDescription: js.UndefOr[String] = js.undefined
  /** Add a "Close" action to the notification. */
  val allowClose: js.UndefOr[Boolean] = js.undefined
  /** An icon shown for the close action. ['power'] */
  val closeIcon: js.UndefOr[String] = js.undefined
  /** The text for the close action. ['Close'] */
  val closeTitle: js.UndefOr[String] = js.undefined
  /** Show the time since the notification was created. [true] */
  val showWhen: js.UndefOr[Boolean] = js.undefined
  /** Android only: one of 'private' (default), 'public' or 'secret'
    * @see [[https://developer.android.com/reference/android/app/Notification.Builder.html#setVisibility(int))]]
    */
  val visibility: js.UndefOr[String] = js.undefined

}
