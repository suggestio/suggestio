package cordova.plugins.notification.local

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.02.2020 19:06
  * Description: Одна нотификация.
  * Этот JSON собирается и на стороне CNL (при сериализации), поэтому НЕ стоит использовать необычные типы.
  *
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications#properties]]
  */
trait CnlToast extends js.Object {

  val id: js.UndefOr[Int] = js.undefined
  val data: js.UndefOr[js.Any] = js.undefined
  val timeoutAfter: js.UndefOr[js.Any] = js.undefined
  val summary: js.UndefOr[String] = js.undefined
  val led: js.UndefOr[CnlLed] = js.undefined
  val clock: js.UndefOr[js.Any] = js.undefined
  val channel: js.UndefOr[String] = js.undefined
  val actions: js.UndefOr[js.Array[String | CnlAction]] = js.undefined

  val text: js.UndefOr[String | js.Array[CnlMessage]] = js.undefined
  val icon: js.UndefOr[String] = js.undefined
  val attachments: js.UndefOr[js.Array[String]] = js.undefined
  val smallIcon: js.UndefOr[String] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val defaults: js.UndefOr[js.Any] = js.undefined
  val launch: js.UndefOr[Boolean] = js.undefined
  val groupSummary: js.UndefOr[Boolean] = js.undefined

  val title: js.UndefOr[String] = js.undefined
  val silent: js.UndefOr[Boolean] = js.undefined
  val progressBar: js.UndefOr[Int] = js.undefined
  val sticky: js.UndefOr[Boolean] = js.undefined
  val vibrate: js.UndefOr[Boolean] = js.undefined
  val priority: js.UndefOr[Int] = js.undefined
  val mediaSession: js.UndefOr[js.Any] = js.undefined
  val foreground: js.UndefOr[Boolean] = js.undefined

  val sound: js.UndefOr[String] = js.undefined    // может быть null
  val trigger: js.UndefOr[CnlTrigger] = js.undefined
  val group: js.UndefOr[Int] = js.undefined
  val autoClear: js.UndefOr[Boolean] = js.undefined
  val lockscreen: js.UndefOr[Boolean] = js.undefined
  val number: js.UndefOr[Int] = js.undefined
  val badge: js.UndefOr[Int] = js.undefined
  val wakeup: js.UndefOr[Boolean] = js.undefined

  val iconType: js.UndefOr[js.Any] = js.undefined

}
