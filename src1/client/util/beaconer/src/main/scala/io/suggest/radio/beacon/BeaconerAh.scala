package io.suggest.radio.beacon

import diode._
import diode.data.Pot
import io.suggest.ble.{BeaconUtil, BeaconsNearby_t, MUidBeacon}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.dev.MOsFamily
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.n2.node.MNodeIdType
import io.suggest.radio.{MRadioData, MRadioSignalTypes, RadioUtil}
import io.suggest.sjs.common.model.MTsTimerId
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.stat.RunningAverage
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import java.time.Instant
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 18:04
  * Description: Controller for radio (or other) beacons scanning system.
  */

object BeaconerAh extends Log {

  /** How long does it take to silently collect initial data on beacons before any conclusions can be drawn.
    * It seems that the most unhurried beacons normally send a request every 1-1.1 sec, but we take a timeout with a margin.
    */
  private def EARLY_INIT_TIMEOUT_MS = 1200

  /** Run garbage collection for lost beacons every N milliseconds. */
  private def GC_LOST_EVERY_MS = 5000

  /**
    * Forget about the beacon if it is not heard for more than a specified period of time.
    * On test beacons, "5 seconds" was not enough, there were false positives.
    * "9 seconds" turned out to be not enough on android at the end of 2020y - the card disappeared & appeared.
    */
  private def FORGET_UNSEEN_AFTER_SECONDS = 15

  /** How many milliseconds after receiving a notification from the beacon, to start checking the beacon map to
    * look for changes in the environment of the surrounding beacons?
    * The timer is needed to smooth out possible fluctuations at a high density of beacons and to reduce the load on the devices.
    */
  private def CHECK_BEACONS_DIRTY_AFTER_MS = 300


  /** Generate beacons nearby report.
    *
    * @param beacons Current raw beacons list.
    * @return Filtered list in unknown order.
    */
  // TODO Delete beacons nearby stuff? Current reaction defined via callback in BleBeaconerAh constructor.
  //      Here is no filtering/sorting, so this function is closer to ~map view.
  def beaconsNearby(beacons: Map[String, MRadioData]) : Seq[(String, MUidBeacon)] = {
    beacons
      .iterator
      .flatMap { case (k, v) =>
        val signal = v.signal.signal
        val res = for {
          uid           <- signal.factoryUid
          accuracyCm    <- v.accuracy
        } yield {
          val nodeIdType = MNodeIdType( uid, signal.typ.nodeType )
          (k, MUidBeacon( nodeIdType, Some(accuracyCm) ))
        }
        if (res.isEmpty)
          logger.log( ErrorMsgs.BEACON_ACCURACY_UNKNOWN, msg = v )
        res
      }
      //.filter { tuple =>
      //  tuple._2.distanceCm < 10000
      // Beacons too far away not interesting.
      // 2017-03-24: Disabled. Measured distance fluctuations are too much (for example, jumps from 20 to 85 meters).
      // Filtering by measured signal distance makes too little sence and too much problems in real-world cases.
      //}
      .toList
  }


  /** Calculate hash fingerprint for currently visible radio-enviroment state.
    *
    * @param beacons Raw beacons list.
    * @return Hash.
    */
  def mkFingerPrint( beacons: Seq[(String, MUidBeacon)] ): Option[Int] = {
    OptionUtil.maybe( beacons.nonEmpty ) {
      if (beacons.lengthIs >= 3) {
        // Distance ignored, when many beacons seen.
        mkFingerPrintRaw( beacons )
      } else {
        // When small count of beacons seen, distance also need to be hashed. For debugging purposes, for example.
        mkFingerPrintDistanced( beacons )
      }
    }
  }


  def mkFingerPrintDistanced( beacons: Seq[(String, MUidBeacon)] ): Int = {
    (for {
      (bcnUid, rep) <- beacons.iterator
      distanceCm <- rep.distanceCm
    } yield {
      bcnUid -> BeaconUtil.quantedDistance( distanceCm )
    })
      .toSeq
      // Sorting by normalized quanted distance and by beacon id. It helps to suppress fluctuations from beacons near.
      .sortBy { case (bcnUid, distQ) =>
        "%04d".format(distQ) + "." + bcnUid
      }
      .map(_._1)
      .hashCode()
  }

  def mkFingerPrintRaw( beacons: Seq[(String, MUidBeacon)] ): Int = {
    beacons
      .iterator
      .map(_._1)
      .toSet
      .hashCode()
  }


  /** Start notify-timer.
    *
    * @return Timestamp id and timer effect.
    */
  private def startNotifyAllTimer(timeoutMs: Int): (MTsTimerId, Effect) = {
    val ts = System.currentTimeMillis()
    val tp = DomQuick.timeoutPromiseT(timeoutMs)( MaybeNotifyAll(ts) )
    val fx = Effect( tp.fut )
    val timerInfo = MTsTimerId(
      timerId = tp.timerId,
      timestamp = ts
    )
    (timerInfo, fx)
  }


  /** Ensure, notify timer is running.
    *
    * @param notifyAllTimer Current notify timer data.
    * @param beacons2 New beacons list.
    * @param envFingerPrintOld Last notify beacons fingerpring hash.
    * @return Optional new timer data with effect.
    */
  private def ensureNotifyAllDirtyTimer(notifyAllTimer: Option[MTsTimerId],
                                        beacons2: Map[String, MRadioData],
                                        envFingerPrintOld: Option[Int] ): (Option[MTsTimerId], Option[Effect]) = {
    if (notifyAllTimer.isEmpty) {
      // Calculate fingerpring hash of updated beacons list.
      val fingerPrint2 = BeaconerAh.mkFingerPrint( BeaconerAh.beaconsNearby(beacons2) )
      if (fingerPrint2 !=* envFingerPrintOld) {
        // Old hash does not match to new hash. Need to start new notify timer.
        val (mts, fx) = BeaconerAh.startNotifyAllTimer( BeaconerAh.CHECK_BEACONS_DIRTY_AFTER_MS )
        Some(mts) -> Some(fx)
      } else {
        None -> None
      }
    } else {
      notifyAllTimer -> None
    }
  }

}


/** Beacons monitoring controller. */
class BeaconerAh[M](
                     dispatcher        : Dispatcher,
                     modelRW           : ModelRW[M, MBeaconerS],
                     bcnsIsSilentRO    : ModelRO[Boolean],
                     osFamilyOpt       : => Option[MOsFamily],
                     beaconApis        : () => LazyList[IBeaconsListenerApi],
                     onNearbyChange    : Option[(BeaconsNearby_t, BeaconsNearby_t) => Option[Effect]] = None,
                   )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  private def _getListenOpts(opts: MBeaconerOpts) = {
    IBeaconsListenerApi.ListenOptions(
      onBeacon = dispatcher(_: RadioSignalsDetected),
      scanMode = opts.scanMode,
    )
  }

  /** Return only available APIs. */
  private def _beaconApisAvailable(): LazyList[IBeaconsListenerApi] = {
    beaconApis()
      .filter { api =>
        api.isApiAvailable
      }
  }


  /** Make effect for detect and activate BLE scanning API.
    * @param opts Beaconer options.
    * @return Optional effect.
    */
  private def startApiActivation( opts: MBeaconerOpts, listenOpts: IBeaconsListenerApi.ListenOptions ): Effect = {
    // Subscribe on first available API. On errors - go to next API on list.
    // Subscription on several/all APIs makes no sense, many of useless notifications will be here.
    Effect {
      // Async folding list of available APIs
      def __foldApisAsync(restApis: Seq[IBeaconsListenerApi]): Future[IBeaconsListenerApi] = {
        restApis.headOption.fold [Future[IBeaconsListenerApi]] {
          val emsg = ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE
          logger.log( emsg, msg = restApis )
          Future.failed( new NoSuchElementException( emsg ) )

        } { bbApi =>
          // Try to activate bluetooth...
          FutureUtil
            .tryCatchFut {
              for {
                // Android: try to pass all checks, and enable bluetooth, if needed.
                // iOS: isEnabled check may fail.
                // So suppress all errors, anyway try listen for beacons.

                // Is bluetooth enabled now?
                isEnabled0 <- bbApi
                  .isPeripheralEnabled()
                  // iOS: It may fail...
                  .recover { case ex =>
                    logger.warn( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, ex, msg = bbApi )
                    false
                  }

                // If Bluetooth disabled, maybe to enable it, if allowed by beaconer options.
                if {
                  val r = isEnabled0 || opts.askEnableBt
                  if (!r) logger.log( ErrorMsgs.BLE_BT_DISABLED, msg = (bbApi, isEnabled0, opts.askEnableBt) )
                  r
                }

                // Next, try enable bluetooth, if disabled and allowed to enable (by options).
                isEnabled2 <- {
                  if (isEnabled0)
                    Future.successful(isEnabled0)
                  else
                    bbApi
                      .enablePeripheral()
                      // iOS: Error is ok. Only listenBeacons() error matters.
                      .recover { case ex =>
                        logger.info( ErrorMsgs.BLE_BEACONS_API_ENABLE_FAILED, ex, msg = bbApi )
                        true
                      }
                }

                // If bluetooth still disabled, there are no reason to start scanning.
                if {
                  val r = isEnabled2
                  if (!r)
                    logger.log( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, msg = (bbApi, r) )
                  r
                }

                // Start to listen for BLE-beacons, because bluetooth looks enabled and ready.
                _ <- bbApi.listenBeacons( listenOpts )

              } yield {
                bbApi
              }
            }
            .recoverWith { case ex: Throwable =>
              // Something gone wrong with current BLE API.
              logger.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, ex, bbApi )

              // Try to safely shut off currently failing API.
              val nextP = Promise[None.type]()
              val runNextFut = nextP.future.flatMap { _ =>
                // Go to next BLE API:
                __foldApisAsync( restApis.tail )
              }

              def __runNext(): Unit =
                if (!nextP.isCompleted)
                  nextP.success( None )

              try {
                bbApi
                  .unListenAllBeacons()
                  .recover { case ex2 =>
                    logger.error( ErrorMsgs.BLE_BEACONS_API_SHUTDOWN_FAILED, ex2, bbApi)
                    null
                  }
                  .foreach { _ =>
                    __runNext()
                  }
              } catch {
                case ex3: Throwable =>
                  logger.error( ErrorMsgs.BLE_BEACONS_API_SHUTDOWN_FAILED, ex3, bbApi)
                  __runNext()
              }
              // Timer for waiting failing API to stop and finalize self.
              DomQuick.setTimeout(2000)(__runNext)

              runNextFut
            }
        }
      }

      // Start folding for available APIs. Return first success API.
      val allApis = _beaconApisAvailable()

      (Future.traverse {
        MRadioSignalTypes
          .values
          .map { radioType =>
            val apisForType = allApis.filter(_.radioSignalType ==* radioType)
            radioType -> apisForType
          }
      } { case (radioType, apisForType) =>
        if (apisForType.isEmpty) {
          Future.successful( None )
        } else {
          __foldApisAsync( apisForType )
            .transform { case tryRes =>
              val tupleRes = tryRes
                .toOption
                .map( radioType -> _ )
              Success( tupleRes )
            }
        }
      })
        .map { resultsSeq =>
          HandleListenRes(
            apisReady = resultsSeq
              .iterator
              .flatten
              .toMap,
          )
        }

    }
  }


  /** Automatic control for GC interval.
    * Can call many times, with correct arguments.
    * TODO Impure method, because stops/starts timer without any Effect.
    *
    * @param beaconsEmpty Is beacons visible list empty?
    *                     If beacons map is empty, timer will be stopped.
    *                     In other cases, timer will be started.
    * @param gcIntervalOpt Current interval from previos step or from state.
    *
    * @return Updated interval id of GC timer.
    */
  def ensureGcInterval(beaconsEmpty: Boolean, gcIntervalOpt: Option[Int]): Option[Int] = {
    gcIntervalOpt.fold {
      // Timer not started.
      if (beaconsEmpty) {
        gcIntervalOpt
      } else {
        // Have some beacons, timer not yet started. Start it.
        val ivlId = DomQuick.setInterval( BeaconerAh.GC_LOST_EVERY_MS ) { () =>
          dispatcher.dispatch( DoGc )
        }
        Some(ivlId)
      }

    } { intervalId =>
      // Have GC-timer.
      if (beaconsEmpty) {
        // No beacons, GC timer makes no sense: nothing to GC
        DomQuick.clearInterval( intervalId )
        None
      } else {
        // Have started gc-timer, have beacons for GC. Continue with no changes.
        gcIntervalOpt
      }
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Radio APIs saying about radio-signals detection.
    case m: RadioSignalsDetected =>
      val v0 = value

      if (!(v0.isEnabled contains[Boolean] true)) {
        logger.info( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } else if (m.signals.isEmpty) {
        if (m.radioType ==* MRadioSignalTypes.WiFi) {
          // WiFi scanner can send empty results to reset pending state.
          if (v0.wifiScanTimer.isPending)
            updatedSilent( MBeaconerS.wifiScanTimer.modify(_.unPending)(v0) )
          else
            noChange
        } else {
          // Bluetooth scanner shouldn't return any empty garbage.
          logger.warn( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = m )
          noChange
        }

      } else {
        val beacons2 = m.signals
          .foldLeft( v0.beacons ) { (beaconsMap0, signalJs) =>
            (for {
              beaconUid <- signalJs.signal.factoryUid
            } yield {
              val distanceOptM = RadioUtil.calculateAccuracy( signalJs.signal )

              val distanceM: Double = distanceOptM getOrElse {
                logger.log(ErrorMsgs.BEACON_ACCURACY_UNKNOWN, msg = signalJs)
                99
              }
              // Distance in centimeters is much simpler and integer.
              val distanceCm = (distanceM * 100).toInt

              val beaconDataOpt0 = beaconsMap0.get( beaconUid )
              val accuracies2 = beaconDataOpt0
                .fold( RunningAverage[Int](6, knownLen = Some(0)) )( _.accuracies )
                .push( distanceCm )

              val beaconData1 = beaconDataOpt0
                .fold( MRadioData.apply _ )( _.copy )
                .apply( signalJs, accuracies2 )

              beaconsMap0 + ((beaconUid, beaconData1))
            })
              .getOrElse( beaconsMap0 )
          }

        // If no notify timer created, do it.
        val (notifyAllTimerOpt2, notifyFxOpt) = BeaconerAh.ensureNotifyAllDirtyTimer( v0.notifyAllTimer, beacons2, v0.envFingerPrint )

        // Ensure GC-timer created:
        val gcIvl2 = ensureGcInterval( beacons2.isEmpty, v0.gcIntervalId )

        val v2 = v0.copy(
          notifyAllTimer  = notifyAllTimerOpt2,
          beacons         = beacons2,
          gcIntervalId    = gcIvl2,
          // If wifi is pending, and wifi-results received, reset pending state.
          wifiScanTimer   = {
            if (
              v0.wifiScanTimer.isPending &&
              (m.radioType ==* MRadioSignalTypes.WiFi)
            ) {
              // Reset wifi "pending" state back to ready.
              v0.wifiScanTimer.unPending
            } else {
              v0.wifiScanTimer
            }
          },
        )

        ah.optionalResult( Some(v2), notifyFxOpt, silent = bcnsIsSilentRO.value )
      }


    // Execute GC action: clean-up state from unused beacons data.
    case DoGc =>
      val v0 = value

      if (v0.beacons.isEmpty) {
        // Nothing to clean-up.
        val v2 = MBeaconerS.gcIntervalId
          .modify( ensureGcInterval(v0.beacons.isEmpty, _) )(v0)
        updated(v2)

      } else {
        val ttlSeconds = BeaconerAh.FORGET_UNSEEN_AFTER_SECONDS
        val nowSeconds = Instant.now().getEpochSecond

        // Collect beacon ids for deletion:
        val keys2delete = (for {
          (k, mbd) <- v0.beacons.iterator
          if {
            val isOk = (nowSeconds - mbd.signal.seenAt.getEpochSecond) < ttlSeconds
            val isToDelete = !isOk
            isToDelete
          }
        } yield k)
          .toSet

        if (keys2delete.isEmpty) {
          // Nothing collected for deletion. Will wait for next GC timer.
          noChange

        } else {
          // Found beacon data for deletion from state.
          val beacons2 = v0.beacons
            .view
            .filterKeys( !keys2delete.contains(_) )
            .toMap

          val (notifyAllTimerOpt2, notifyFxOpt) = BeaconerAh.ensureNotifyAllDirtyTimer( v0.notifyAllTimer, beacons2, v0.envFingerPrint )

          // Check if have any beacons after cleanup. If no, gc-timer may be stopped.
          val gcIvl2 = if (keys2delete.size ==* v0.beacons.size) {
            // Looks like, all beacons dropped. Drop GC-timer.
            ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId)
          } else {
            v0.gcIntervalId
          }

          val v2 = v0.copy(
            notifyAllTimer  = notifyAllTimerOpt2,
            beacons         = beacons2,
            gcIntervalId    = gcIvl2
          )
          ah.updatedSilentMaybeEffect(v2, notifyFxOpt)
        }
      }


    // Notification timeout. Do notify, if beacons state have changes.
    case m: MaybeNotifyAll =>
      val v0 = value

      def __maybeRmTimer() = {
        Option.when( v0.notifyAllTimer.nonEmpty ) {
          MBeaconerS.notifyAllTimer.set( None )(v0)
        }
      }

      if (v0.notifyAllTimer.exists(_.timestamp ==* m.timestamp)) {
        // Expected timeout. Re-calculate beacon state hash.
        val beaconsNearby = BeaconerAh.beaconsNearby( v0.beacons )
        val fpr2 = BeaconerAh.mkFingerPrint( beaconsNearby )

        def __onChangeFxOpt(v2: MBeaconerS) =
          onNearbyChange
            .flatMap( _(v0.nearbyReport, v2.nearbyReport) )

        if (fpr2 !=* v0.envFingerPrint) {
          // Hash changed. Need to make updated beacons report.
          val v2 = v0.copy(
            envFingerPrint = fpr2,
            notifyAllTimer = None,
            nearbyReport   = beaconsNearby.map(_._2),
          )
          // silent, because onChange in constructor, so circuit subscription now impossible (for performance reasons).
          ah.updatedSilentMaybeEffect( v2, __onChangeFxOpt(v2) )

        } else {
          // Nothing changed or changes too small. Do not call onChange.
          val v2Opt = __maybeRmTimer()
          // If opts.oneShot run, onChange must be called always.
          val fxOpt = OptionUtil.maybeOpt( v0.opts.oneShot )(
            __onChangeFxOpt(v2Opt getOrElse v0)
          )
          this.optionalResult( v2Opt, fxOpt, silent = true )
        }

      } else {
        // Unexpected timer, ignore.
        logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = m )
        val v2 = __maybeRmTimer()
        v2.fold(noChange)(updatedSilent)
      }


    // Enable/disable of BLE Beaconer controller.
    case m: BtOnOff =>
      val v0 = value

      // Enabled or disabled now?
      val isEnabledNow = v0.isEnabled contains[Boolean] true
      val isEnabled2 = m.isEnabled contains[Boolean] true
      def isDisabled2 = m.isEnabled contains[Boolean] false
      def hasBle2 = v0.hasBle orElse v0.hasBle.pending()

      if (
        // First, check for duplicate on/off action with different beaconer options.
        // Here may be isEnabled.pending -- it is ok, if duplicate actionis too frequent.
        isEnabled2 && isEnabledNow &&
        (m.opts !=* v0.opts)
      ) {
        // Update options only.
        var modF = MBeaconerS.opts set m.opts

        val fxOpt = for {
          apis <- v0.bleBeaconsApis.toOption
          // If scanning options changed, need to restart scan with new options.
          listenOpts2 = _getListenOpts( m.opts )
          apisNeedRestart = apis
            .values
            .filter( _.isScannerRestartNeededSettingsOnly( _getListenOpts(v0.opts), listenOpts2, osFamilyOpt ) )
          if apisNeedRestart.nonEmpty
        } yield {
          modF = modF andThen MBeaconerS.isEnabled.modify(_.pending())
          Effect {
            Future.traverse( apisNeedRestart ) { api =>
              api
                .unListenAllBeacons()
                .flatMap { _ =>
                  api.listenBeacons( listenOpts2 )
                }
            }
              .transform { tryRes =>
                val tryEnabled = tryRes.map(_ => true)
                Success( BtOnOffFinish(tryEnabled) )
              }
          }
        }

        val v2 = modF(v0)
        ah.updatedSilentMaybeEffect( v2, fxOpt )

      } else if (v0.isEnabled.isPending) {
        // Pending with re-enable or re-disable.
        if (!isEnabledNow && isDisabled2) {
          // Pending with re-disable. Ignore.
          logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.isEnabled) )
          noChange

        } else if (
          (isEnabledNow && isDisabled2) ||
          (!isEnabledNow && isEnabled2)
        ) {
          // If BLE-scanning is powering on, but shut down action received, need to wait scanning ready, and when - to disable scan.
          val v2 = (MBeaconerS.afterOnOff set Some(m.toEffectPure))(v0)
          updatedSilent(v2)

        } else {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled, v0.opts) )
          noChange
        }

      } else if (!isEnabledNow && isEnabled2) {
        // !isEnabledNow: Possibly, also isEnabledNow=Pot.empty - for very first run.
        // To activate BleBeaconer: start API scanning subscription:

        // Beacon scan API subscription Effect:
        val fx = startApiActivation( m.opts, _getListenOpts(m.opts) )

        val v2 = v0.copy(
          isEnabled     = v0.isEnabled
            .ready(true)
            .pending(),
          bleBeaconsApis = v0.bleBeaconsApis.pending(),
          // Usually, here None - because no beacons expected here.
          gcIntervalId  = ensureGcInterval( v0.beacons.isEmpty, v0.gcIntervalId ),
          opts          = m.opts,
          hasBle        = hasBle2,
        )
        updated( v2, fx )

      } else if (isEnabledNow && isDisabled2) {
        // Stop notify timer TODO Wrap into effect.
        for (timerInfo <- v0.notifyAllTimer)
          DomQuick.clearTimeout( timerInfo.timerId )

        var fxAcc = List.empty[Effect]

        // Stop BleBeaconer: shut down API, stop other timers, etc.
        for {
          bbApis <- v0.bleBeaconsApis.iterator
        } {
          fxAcc ::= Effect {
            Future.traverse( bbApis.values ) { bbApi =>
              bbApi
                .unListenAllBeacons()
            }
              .transform { tryRes =>
                for (ex <- tryRes.failed)
                  logger.error( ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE, ex, m )
                val action = BtOnOffFinish(
                  tryEnabled = tryRes.map(_ => false)
                )
                Success(action)
              }
          }
        }

        // Stop wifi timer, if any
        for {
          timerId <- v0.wifiScanTimer
        } {
          fxAcc ::= Effect.action {
            DomQuick.clearInterval( timerId )
            DoNothing
          }
        }

        val beacons2: Map[String, MRadioData] = {
          Map.empty
        }
        val bcnsNearby2 = BeaconerAh.beaconsNearby( beacons2 )

        // Make new state:
        val v2 = v0.copy(
          isEnabled         = v0.isEnabled
            .ready(false)
            .pending(),
          notifyAllTimer    = None,
          envFingerPrint    = BeaconerAh.mkFingerPrint( bcnsNearby2 ),
          bleBeaconsApis    = Pot.empty,
          // Stop gc-timer.
          gcIntervalId      = ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId),
          opts              = m.opts,
          nearbyReport      = bcnsNearby2.map(_._2),
          beacons           = beacons2,
          hasBle            = hasBle2,
          wifiScanTimer     = Pot.empty,
        )

        ah.updatedMaybeEffect( v2, fxAcc.mergeEffects )

      } else {
        // Already enabled, or already disabled, or m.isEnabled == None.
        // But if hasBle.isEmpty, need to check for bluetooth availability on current device.
        if (v0.hasBle.isEmpty) {
          val hasBleFx = Effect.action {
            val hasBlePot2 = v0.hasBle withTry Try {
              _beaconApisAvailable()
                .exists(_.radioSignalType ==* MRadioSignalTypes.BluetoothEddyStone)
            }
            HasBleRes( hasBlePot2 )
          }
          val v2Opt = Option.when( !v0.hasBle.isPending ) {
            MBeaconerS.hasBle.modify(_.pending())(v0)
          }
          ah.optionalResult( v2Opt, Some(hasBleFx), silent = true )

        } else {
          logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled) )
          noChange
        }
      }


    // API events subscription result.
    case m: HandleListenRes =>
      val v0 = value

      if (!v0.bleBeaconsApis.isPending) {
        // No API subscription expected.
        logger.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, msg = m )
        if (m.apisReady.isEmpty) {
          noChange
        } else {
          val fx = Effect {
            Future
              .traverse( m.apisReady.valuesIterator )( _.unListenAllBeacons() )
              .map( _ => DoNothing )
          }
          effectOnly( fx )
        }

      } else if (m.apisReady.isEmpty && v0.opts.offIfNoApi) {
        // API activation error.
        val beacons2 = Map.empty[String, MRadioData]
        val gcTimer2 = ensureGcInterval( beacons2.isEmpty, v0.gcIntervalId )
        logger.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, msg = m )
        val ex = new IllegalStateException( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED )

        val falsePot2 = v0.isEnabled.ready(false)
        val v2 = v0.copy(
          isEnabled         = falsePot2,
          bleBeaconsApis    = Pot.empty.fail( ex ),
          notifyAllTimer    = None,
          beacons           = beacons2,
          gcIntervalId      = gcTimer2,
          afterOnOff        = None,
          hasBle = v0.hasBle
            .orElse( falsePot2 )
            .fail( ex ),
        )
        ah.updatedMaybeEffect( v2, v0.afterOnOff )

      } else {
        // Beaconer is enabled and expecting for api readyness.
        // API ready. Start initial notify timer.
        val (timerInfo, timerFx) = BeaconerAh.startNotifyAllTimer( BeaconerAh.EARLY_INIT_TIMEOUT_MS )
        val truePot2 = v0.isEnabled ready true
        val v2 = v0.copy(
          notifyAllTimer    = Some(timerInfo),
          isEnabled         = truePot2,
          bleBeaconsApis    = v0.bleBeaconsApis.ready( m.apisReady ),
          gcIntervalId      = ensureGcInterval(v0.beacons.isEmpty, v0.gcIntervalId),
          afterOnOff        = None,
          hasBle            = truePot2,
        )

        val timerFx2 = if (v0.opts.oneShot)
          timerFx >> Effect.action(
            BtOnOff(
              isEnabled = OptionUtil.SomeBool.someFalse,
              opts = v2.opts,
            )
          )
        else
          timerFx

        val fx = (timerFx2 :: v0.afterOnOff.toList)
          .mergeEffects
          .get

        updated(v2, fx)
      }


    // Finished (de)initialization of beaconer.
    case m: BtOnOffFinish =>
      val v0 = value

      def __maybeNoChange =
        v0.afterOnOff.fold( noChange ) { afterOnOffFx =>
          val v2 = (MBeaconerS.afterOnOff set None)(v0)
          updatedSilent(v2, afterOnOffFx)
        }

      if (!v0.isEnabled.isPending) {
        // Should not happend: pending should be here.
        logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = m )
        __maybeNoChange

      } else {
        // Beaconer under initialization.
        var v2F = MBeaconerS.isEnabled
          .modify( _ withTry m.tryEnabled )

        if (v0.afterOnOff.nonEmpty)
          v2F = v2F andThen (MBeaconerS.afterOnOff set None)

        def __updatedMaybeEffect =
          ah.updatedMaybeEffect( v2F(v0), v0.afterOnOff )

        m.tryEnabled.fold(
          {_ =>
            // Error during initialization.
            __updatedMaybeEffect
          },
          {isEnabled2 =>
            if (v0.isEnabled contains[Boolean] isEnabled2) {
              // Finished as expected.
              __updatedMaybeEffect
            } else {
              // Unrelated finish action. Expected inverted readyness (OFF instead of ON, vice versa).
              logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled) )
              __maybeNoChange
            }
          }
        )
      }


    // ReSet hasBle flag.
    case m: HasBleRes =>
      val v0 = value
      if (v0.hasBle ==* m.hasBle) {
        noChange

      } else {
        val v2 = (MBeaconerS.hasBle set m.hasBle)(v0)
        // Silent, because no model value circuit subscription expected here.
        updatedSilent(v2)
      }

  }

}
