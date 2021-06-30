package io.suggest.sc

import io.suggest.radio.beacon.{BtOnOff, MBeaconerOpts, RadioSignalsDetected}
import io.suggest.log.Log
import io.suggest.pick.JsBinaryUtil
import io.suggest.radio.{MRadioSignal, MRadioSignalJs, MRadioSignalTypes}
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


  @JSExport
  def throwSnack(): Unit = {
    _d {
      val a = DoNothing
      Sc3Module.ref.sc3Circuit.handleEffectProcessingError(a, new NoSuchElementException)
      a
    }
  }


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
      _initializeBeaconer()

      val rnd = new Random()
      val ivlIds = (1 to count)
        .foldLeft( List.empty[Int] ) { (acc, i) =>
          val bcnTxPower = -70 - rnd.nextInt(30)
          // Генерация псевдо-случайного id вида "aa112233445566778899-000000000456"
          val tailId = 100000000000L + rnd.nextLong(1000000000L) + i
          val bcnUid = "bb112233445566778899-" + tailId.toString
          val ivlId = DomQuick.setInterval( 800 + rnd.nextInt(800) ) { () =>
            val radioType = MRadioSignalTypes.BluetoothEddyStone
            val action = RadioSignalsDetected(
              signals = MRadioSignalJs(
                signal = MRadioSignal(
                  rssi          = Some( -30 - rnd.nextInt(70) ),
                  rssi0         = Some( bcnTxPower ),
                  factoryUid    = Some( bcnUid ),
                  typ           = radioType,
                ),
              ) :: Nil,
              radioType = radioType,
            )
            _d( action )
          }
          ivlId :: acc
        }
      _beaconsIntervalIdsUnd = js.defined( ivlIds )
    }

    DoNothing
  }

  /** Ensure BeaconerAh ready to accept fake RadioSignalDetected() messages. */
  private def _initializeBeaconer(): Unit = {
    _d {
      BtOnOff(
        isEnabled = Some(true),
        opts = MBeaconerOpts(
          askEnableBt = false,
          oneShot     = false,
          offIfNoApi  = false,
        ),
      )
    }
  }


  private var _wifiIntervalIdU: js.UndefOr[Int] = js.undefined

  private def _wifiEmitter(ids: Seq[String]) = _d {
    // Cleanup currently running timers, if any:
    for {
      ivlId <- _wifiIntervalIdU
    } yield {
      DomQuick.clearInterval( ivlId )
    }
    _wifiIntervalIdU = js.undefined

    // Activate timed wifi-scan-results imitation:
    if (ids.iterator.nonEmpty) {
      _initializeBeaconer()

      val rnd = new Random()
      // For wifi scan imitation - used single interval for list of networks.
      _wifiIntervalIdU = DomQuick.setInterval( (2 + rnd.nextInt(4)) * 1000 ) { () =>
        val radioType = MRadioSignalTypes.WiFi
        val action = RadioSignalsDetected(
          signals = ids
            .iterator
            .zipWithIndex
            .map { case (macAddr, i) =>
              MRadioSignalJs(
                signal = MRadioSignal(
                  rssi        = Some( -5 - rnd.nextInt(90) ),
                  typ         = radioType,
                  factoryUid  = Some( macAddr ),
                  customName  = Some( "WiFi Emit #" + i ),
                )
              )
            }
            .to( LazyList ),
          radioType = radioType,
        )
        _d( action )
      }
    }

    DoNothing
  }

  /** Run emission of Wi-Fi detection signals. */
  @JSExport
  def wifiEmit(count: Int = 0): Unit = {
    _wifiEmitter(
      ids = {
        if (count <= 0) {
          Nil
        } else {
          val rnd = new Random()
          val macAddrSeed = rnd.nextInt( Int.MaxValue )

          Iterator
            .from( 1 )
            .take( count )
            .map { i =>
              JsBinaryUtil.toHexString( macAddrSeed * i, 6 )
            }
            .to( List )
        }
      }
    )
  }

  /** Emit wi-fi signals with these MAC-addrs. */
  @JSExport
  def wifiEmitById(ids: String*): Unit = _wifiEmitter( ids )

}
