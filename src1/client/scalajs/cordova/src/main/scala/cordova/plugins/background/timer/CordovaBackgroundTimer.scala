package cordova.plugins.background.timer

import io.suggest.sjs.JsApiUtil
import io.suggest.err.ToThrowableJs._

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.04.2020 22:08
  * Description: JS API поддержки cordova-background-timer.
  * @see [[https://github.com/paladinarcher/cordova-background-timer]]
  */
@js.native
trait CordovaBackgroundTimer extends js.Object {

  /** Подписка на срабатывание таймера. */
  def onTimerEvent(callback: js.Function0[Unit]): Unit = js.native

  /** Запуск таймера. */
  def start(success: js.Function0[Unit] = js.native,
            error: js.Function1[js.Any, Unit] = js.native,
            settings: CordovaBackgroundTimerSettings = js.native,
           ): Unit = js.native

  /** Остановка таймера. */
  def stop( success: js.Function0[Unit] = js.native,
            error: js.Function1[js.Any, Unit] = js.native ): Unit = js.native

}


object CordovaBackgroundTimer {

  implicit final class CdvBgTimer4s( private val cdvBgTimer: CordovaBackgroundTimer ) extends AnyVal {

    def onTimerEventF( callback: => Unit ): Unit =
      cdvBgTimer.onTimerEvent( () => callback )

    def startF( settings: js.UndefOr[CordovaBackgroundTimerSettings] = js.undefined ): Future[Unit] =
      JsApiUtil.call0ErrFut[js.Any]( cdvBgTimer.start(_, _, settings.asInstanceOf[CordovaBackgroundTimerSettings]) )

    def stopF(): Future[Unit] =
      JsApiUtil.call0ErrFut[js.Any]( cdvBgTimer.stop )

  }

}
