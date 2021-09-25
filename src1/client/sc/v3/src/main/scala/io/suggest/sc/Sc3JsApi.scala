package io.suggest.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.radio.beacon.{BtOnOff, MBeaconerOpts, RadioSignalsDetected}
import io.suggest.log.Log
import io.suggest.pick.JsBinaryUtil
import io.suggest.radio.{MRadioSignal, MRadioSignalJs, MRadioSignalTypes}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.dia.first.{MWzFrames, MWzPhases, WzDebugView}
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

  private def _ifDev[T](f: => T): Option[T] =
    Option.when( scalajs.LinkingInfo.developmentMode )(f)
  private def _ifDevOpt[T](f: => Option[T]): Option[T] =
    OptionUtil.maybeOpt( scalajs.LinkingInfo.developmentMode )(f)

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
  def throwSnack(): Unit = _ifDev {
    Sc3Module.ref.sc3Circuit.handleEffectProcessingError( DoNothing, new NoSuchElementException )
  }

  @JSExport
  def emitCancel(intervalIds: Any): Unit = _ifDev {
    intervalIds match {
      case opt: Option[_] =>
        emitCancel( opt.iterator )
      case intervalId: Int =>
        DomQuick.clearInterval( intervalId )
      case arr: IterableOnce[_] =>
        arr.iterator.foreach( emitCancel )
      case arr: js.Array[_] =>
        emitCancel( arr.iterator )
    }
  }

  /** Запуск/остановка эмиссии сигналов маячков. */
  @JSExport
  def beaconsEmit(count: Int = 0) = _ifDevOpt {
    // Запуск генерации сигналов eddystone-uid.
    Option.when(count > 0) {
      _initializeBeaconer()

      val rnd = new Random()
      (1 to count)
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
    }
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

  /** WiFi networks example names for emitter. */
  private def _WIFI_SSID_EXAMPLES = List[String](
    "TENIY",
    "190",
    "GUVD",
    "kv222",
    "DLINK_DIR_235235",
    "CF-NT-WIFI",
    "TP-Link_363F",
    "Android-5323fff2",
    "DOM-2.4G",
    "HELLO WORLD NET",
    "Keenetic-664444",
    "Zyxel-535fff",
    "THE XATA XATA XATA XATA XAXAXATA",
  )

  private def _wifiEmitter(ids: Seq[String], rnd: Random = new Random()) = _ifDevOpt {
    // Activate timed wifi-scan-results imitation:
    Option.when( ids.iterator.nonEmpty ) {
      _initializeBeaconer()

      // For wifi scan imitation - used single interval for list of networks.
      if (ids.nonEmpty) {
        val seed = rnd.nextInt( 20 )
        DomQuick.setInterval( (2 + rnd.nextInt(4)) * 1000 ) { () =>
          val radioType = MRadioSignalTypes.WiFi
          val exampleNames = _WIFI_SSID_EXAMPLES
          val exampleNamesCount = exampleNames.length
          val action = RadioSignalsDetected(
            signals = ids
              // randomize list:
              .iterator
              .map { _ -> (rnd.nextInt(1000) - 600) }
              .filter(_._2 > 0)   // Add some signals miss randomly...
              .toSeq
              .sortBy(_._2)
              .iterator
              .map(_._1)
              // Emulate radio signals:
              .zipWithIndex
              .map { case (macAddr, i) =>
                MRadioSignalJs(
                  signal = MRadioSignal(
                    rssi        = Some( -5 - rnd.nextInt(90) ),
                    typ         = radioType,
                    factoryUid  = Some( macAddr ),
                    customName  = Some {
                      val index = (seed + i) % exampleNamesCount
                      var name = exampleNames( index )
                      if (i > exampleNamesCount)
                        name = name + " #" + i
                      name
                    },
                  ),
                )
              }
              .to( LazyList ),
            radioType = radioType,
          )

          if (action.signals.nonEmpty)
            _d( action )
        }
      }
    }
  }

  /** Run emission of Wi-Fi detection signals. */
  @JSExport
  def wifiEmit(count: Int = 0) = {
    val rnd = new Random()
    _wifiEmitter(
      ids = {
        if (count <= 0) {
          Nil
        } else {
          val macAddrSeed = rnd.nextInt( Int.MaxValue )

          Iterator
            .from( 1 )
            .take( count )
            .map { i =>
              JsBinaryUtil.toHexString( macAddrSeed * i, 6 )
            }
            .to( List )
        }
      },
      rnd = rnd,
    )
  }

  /** Emit wi-fi signals with these MAC-addrs. */
  @JSExport
  def wifiEmitById(ids: String*) = _wifiEmitter( ids )


  /** Return current showcase root state. */
  @JSExport
  def rootState() = _ifDev( Sc3Module.ref.sc3Circuit.rootRW.value ).orNull

  @JSExport
  def gridAdsTree(): String = {
    rootState()
      .grid.core.ads.adsTreePot
      .fold("")(_.drawTree)
  }


  /** Render wzFirst dialog part on the screen. */
  @JSExport
  def wz1DebugView(phase: String, frame: String): Unit = _d {
    WzDebugView(
      phase = MWzPhases.withNameInsensitive( phase ),
      frame = MWzFrames.withNameInsensitive( frame ),
    )
  }

}
