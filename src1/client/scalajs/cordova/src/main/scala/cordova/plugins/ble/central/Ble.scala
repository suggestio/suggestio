package cordova.plugins.ble.central

import cordova.plugins.ble.central.central.Services_t

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.18 15:32
  * Description: cordova api facade to cordova-plugin-ble-central.
  *
  * @see [[https://github.com/don/cordova-plugin-ble-central]]
  */
@JSGlobal("ble")
@js.native
object Ble extends js.Object {

  def scan(
            services: Services_t,
            seconds: Int,
            success: js.Function1[BtDevice, Unit],
            failure: js.Function1[js.Any, Unit] = js.native
          ): Unit = js.native

  def startScan(
                 services: Services_t,
                 success: js.Function1[BtDevice, _],
                 failure: js.Function1[js.Any, Unit] = js.native
               ): Unit = js.native

  def startScanWithOptions(
                            services: Services_t,
                            options: StartScanOptions,
                            success: js.Function1[BtDevice, _],
                            failure: js.Function1[js.Any, Unit] = js.native
                          ): Unit = js.native

  def stopScan(
                success: js.Function0[Unit] = js.native,
                failure: js.Function1[js.Any, Unit] = js.native
              ): Unit = js.native

  def isEnabled(
                 enabled: js.Function0[Unit],
                 notEnabled: js.Function0[Unit]
               ): Unit = js.native

  /** iOS not supported. */
  def enable(
              success: js.Function0[Unit],
              refused: js.Function0[Unit]
            ): Unit = js.native

  // ...

}
