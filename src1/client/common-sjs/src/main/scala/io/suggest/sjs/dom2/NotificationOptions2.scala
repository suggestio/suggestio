package io.suggest.sjs.dom2

import org.scalajs.dom.experimental.NotificationOptions

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.04.2020 0:04
  * Description: Замена sjs.dom.experimental.NotificationOptions
  */
trait NotificationOptions2 extends js.Object {

  val body: js.UndefOr[String] = js.undefined
  val dir: js.UndefOr[String] = js.undefined
  val icon: js.UndefOr[String] = js.undefined
  val lang: js.UndefOr[String] = js.undefined
  val noscreen: js.UndefOr[Boolean] = js.undefined
  val renotify: js.UndefOr[Boolean] = js.undefined
  val silent: js.UndefOr[Boolean] = js.undefined
  val sound: js.UndefOr[String] = js.undefined
  val sticky: js.UndefOr[Boolean] = js.undefined
  val tag: js.UndefOr[String] = js.undefined
  val vibrate: js.UndefOr[js.Array[Double]] = js.undefined
  val image: js.UndefOr[String] = js.undefined
  val badge: js.UndefOr[String] = js.undefined
  val onclick: js.UndefOr[js.Function0[Any]] = js.undefined
  val onerror: js.UndefOr[js.Function0[Any]] = js.undefined
  // deprecated
  val onshow: js.UndefOr[js.Function0[Any]] = js.undefined
  val onclose: js.UndefOr[js.Function0[Any]] = js.undefined

}

object NotificationOptions2 {
  @inline implicit def toNotificationOptions( no2: NotificationOptions2 ): NotificationOptions =
    no2.asInstanceOf[NotificationOptions]
}
