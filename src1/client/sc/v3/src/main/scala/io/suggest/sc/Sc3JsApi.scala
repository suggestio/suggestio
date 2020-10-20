package io.suggest.sc

import io.suggest.ble.BeaconDetected
import io.suggest.ble.eddystone.MEddyStoneUid
import io.suggest.log.Log
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.{SetDebug, UpdateUnsafeScreenOffsetBy}
import io.suggest.sc.m.inx.{GetIndex, MScSwitchCtx, UnIndex}
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.{DAction, DoNothing}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.Random

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.2020 13:29
  * Description: Plain JS api for debugging from js-console.
  *
  * Используется длинное неповторимое глобальное имя, т.к. короткое "Sc" приводило к name clash после js-минификации.
  */
@JSExportTopLevel("___Sio___Sc___")
object Sc3JsApi extends Log {

  // Инлайнинга нет, т.к. код и так срезается в ClosureCompiler при production build.
  private def _d( action: => DAction ) =
    if (scalajs.LinkingInfo.developmentMode)
      Sc3Module.ref.sc3Circuit.dispatch( action )

  @JSExport
  def unsafeOffsetAdd(incDecBy: Int): Unit =
    _d( UpdateUnsafeScreenOffsetBy(incDecBy) )

  @JSExport
  def unIndex(): Unit =
    _d( UnIndex )

  @JSExport
  def reIndex(): Unit = _d {
    GetIndex(MScSwitchCtx(
      indexQsArgs = MScIndexArgs(
        geoIntoRcvr = true,
      ),
      showWelcome = false,
    ))
  }

  @JSExport
  def debug(isDebug: Boolean): Unit =
    _d( SetDebug( isDebug ) )


  /** Хранилка состояния интервалов генерации сигналов от маячков: */
  private var _beaconsIntervalIdsUnd: js.UndefOr[Iterable[Int]] = js.undefined

  /** Запуск/остановка эмиссии сигналов маячков. */
  @JSExport
  def beaconsEmit(count: Int = 0): Unit = _d {
    // Очистить текущие интервалы:
    for {
      ivlIds <- _beaconsIntervalIdsUnd
      ivlId  <- ivlIds
    } {
      DomQuick.clearInterval( ivlId )
    }
    _beaconsIntervalIdsUnd = js.undefined

    // Запуск генерации сигналов eddystone-uid.
    if (count > 0) {
      val rnd = new Random()
      val ivlIds = (1 to count)
        .foldLeft( List.empty[Int] ) { (acc, i) =>
          val bcnTxPower = -70 - rnd.nextInt(30)
          // Генерация псевдо-случайного id вида "aa112233445566778899-000000000456"
          val tailId = 100000000000L + rnd.nextLong(1000000000L) + i
          val bcnUid = "bb112233445566778899-" + tailId.toString
          val ivlId = DomQuick.setInterval( 800 + rnd.nextInt(800) ) { () =>
            val action = BeaconDetected(
              signal = MEddyStoneUid(
                rssi    = -30 - rnd.nextInt(70),
                txPower = bcnTxPower,
                uid     = bcnUid,
              ),
            )
            _d( action )
          }
          ivlId :: acc
        }
      _beaconsIntervalIdsUnd = js.defined( ivlIds )
    }

    DoNothing
  }

}
