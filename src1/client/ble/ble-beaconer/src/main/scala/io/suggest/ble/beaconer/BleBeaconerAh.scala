package io.suggest.ble.beaconer

import diode._
import diode.data.Pot
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.{BeaconDetected, BeaconUtil, BeaconsNearby_t, MUidBeacon}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.radio.RadioUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.sjs.common.model.MTsTimerId
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.stat.RunningAverage
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 18:04
  * Description: Контроллер системы наблюдения за маячками.
  * Может встраивается в основную circuit.
  *
  * Suspend-режима нет: для заморозки - выключение, и маячки постепенно будут выкинуты.
  */

object BleBeaconerAh extends Log {

  /**
    * Сколько времени нужно собирать молча начальные данные по маячкам, прежде чем можно рот открывать.
    * Вроде самые тормозные маяки в норме шлют запрос каждые 1-1.1 сек, но берём по-больше.
    */
  private def EARLY_INIT_TIMEOUT_MS = 1200

  /** Запускать поиск забытых маячков каждые N миллисекунд. */
  private def GC_LOST_EVERY_MS = 5000

  /**
    * Забывать о маячке, если его не слышно более указанного промежутка.
    * На тестовых маячках 5000 было маловато, были ложные срабатывания.
    * 9000 оказалось маловато для андройда в конце 2020 - карточка исчезала-появлась.
    */
  private def FORGET_UNSEEN_AFTER = 15000

  /**
    * Через сколько ms после получения уведомления от маячка, запускать проверку карты маячков на предмет
    * поиска измений окружающих маячков?
    * Таймер нужен, чтобы сгладить возможные флуктуации при высокой плотности маячков
    * и снизить нагрузку на устройства.
    */
  private def CHECK_BEACONS_DIRTY_AFTER_MS = 300



  /** Собрать маячки поблизости.
    *
    * @param beacons Текущий список маячков.
    * @return Список близких маячков в неопределённом порядке.
    */
  // TODO Упразднить beacons nearby? реакция через callback в конструкторе BleBeaconerAh, состояние полностью immutable,
  //      и фильтрации или сортировки тут нет, это скорее тяжелый map view
  def beaconsNearby(beacons: Iterable[(String, MBeaconData)]) : Seq[(String, MUidBeacon)] = {
    beacons
      .iterator
      .flatMap { case (k, v) =>
        val res = for {
          uid           <- v.detect.signal.beaconUid
          accuracyCmD   <- v.accuracies/*.stripExtemes()*/.average
          accuracyCm = accuracyCmD.toInt
        } yield {
          (k, MUidBeacon(uid, Some(accuracyCm) ))
        }
        if (res.isEmpty)
          logger.log( ErrorMsgs.BEACON_ACCURACY_UNKNOWN, msg = v )
        res
      }
      //.filter { tuple =>
      // Маячки на значительном расстоянии уже неинтересны.
      // 2017.mar.24: Отключено. Тут стояло 20м, но в первом же реальном магазине (ТК Гулливер) выяснилось
      // что из-за всяких разных факторов ощущаемые расстояния колеблятся ОТ 20 метров до 85 метров. А это значит,
      // что фильтровать по дальности вообще смысла мало, т.к. кроме проблем это ничего не сулит в реальной обстановке.
      //tuple._2.distanceCm < 10000
      //}
      .toList
  }


  /** Рассчёт хэш-суммы отпечатка текущего состояния наблюдаемых маячков.
    *
    * @param beacons Маячки.
    * @return Хэш маячков.
    */
  def mkFingerPrint( beacons: Seq[(String, MUidBeacon)] ): Option[Int] = {
    OptionUtil.maybe( beacons.nonEmpty ) {
      if (beacons.lengthIs >= 3) {
        // Когда в эфире много маячков, дистанция игнорируется.
        mkFingerPrintRaw( beacons )
      } else {
        // Когда в эфире полтора маячка, учитываем дистанцию:
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
      // Сортируем по нормализованному кванту расстояния и по id маячка, чтобы раздавить флуктуации от рядом находящихся маячков.
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


  /** Запустить notify-таймер.
    *
    * @return Таймштамп и эффект ожидания таймера уже запущенного таймера.
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


  /** Убедиться, что таймер notify-all запущен, если это требуется.
    *
    * @param notifyAllTimer Текущии (исходные) данные по таймеру notify-all
    * @param beacons2 Новый список маячков.
    * @param envFingerPrintOld Отпечаток маячков во время последнего уведомления.
    * @return Опциональный новый таймер и опциональный эффект.
    */
  private def ensureNotifyAllDirtyTimer(notifyAllTimer: Option[MTsTimerId],
                                        beacons2: Map[String, MBeaconData],
                                        envFingerPrintOld: Option[Int] ): (Option[MTsTimerId], Option[Effect]) = {
    if (notifyAllTimer.isEmpty) {
      // Посчитать контрольную сумму обновлённого списка маячков.
      val fingerPrint2 = BleBeaconerAh.mkFingerPrint( BleBeaconerAh.beaconsNearby(beacons2) )
      if (fingerPrint2 !=* envFingerPrintOld) {
        // Новая контрольная сумма маячков отличается от ранее сохранённой в состоянии. Надо запустить таймер для последующего уведомления внешних систем.
        val (mts, fx) = BleBeaconerAh.startNotifyAllTimer( BleBeaconerAh.CHECK_BEACONS_DIRTY_AFTER_MS )
        Some(mts) -> Some(fx)
      } else {
        None -> None
      }
    } else {
      notifyAllTimer -> None
    }
  }

}


/** Контроллер мониторинга маячков. */
class BleBeaconerAh[M](
                        dispatcher        : Dispatcher,
                        modelRW           : ModelRW[M, MBeaconerS],
                        bcnsIsSilentRO    : ModelRO[Boolean],
                        onNearbyChange    : Option[(BeaconsNearby_t, BeaconsNearby_t) => Option[Effect]] = None,
                      )
  extends ActionHandler(modelRW)
  with Log
{ ah =>


  /** Запуск поиска и активации Ble Beacon API.
    * @param askEnableBt Если API доступно, но BT выключен, то запрашивать юзера включение BT?
    * @return Опциональный эффект.
    */
  private def startApiActivation( askEnableBt: Boolean ): Effect = {
    // Подписаться на первое доступное API. При ошибках - переходить к следующему API по списку.
    // На все API нет смысла подписываться: тогда будут приходить ненужные уведомления.
    Effect {
      // Асинхронная свёрстка списка доступных API'шек.
      def __foldApisAsync(restApis: Seq[IBleBeaconsApi]): Future[IBleBeaconsApi] = {
        restApis.headOption.fold [Future[IBleBeaconsApi]] {
          val emsg = ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE
          logger.log( emsg, msg = restApis )
          Future.failed( new NoSuchElementException( emsg ) )

        } { bbApi =>
          // Надо бы активировать bluetooth, раз уж пошла активация системы.
          FutureUtil
            .tryCatchFut {
              for {
                // Для андройд: надо пройти все проверки, включить блютус при необходимости.
                // Для iOS: все проверки и попытка включения могут зафейлится вообще (до listenBeacons).
                // Т.к. платформа влияет на всю ветвь isEnabled, ветка включения bluetooth через API опциональна:
                // подавляем все возможные ошибки, но всё равно пытаемся началь слушать маячки.

                // Узнать, включён ли bluetooth сейчас?
                isEnabled0 <- bbApi
                  .isBleEnabled()
                  // iOS: подавить любые возможные ошибки:
                  .recover { case ex =>
                    logger.warn( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, ex, msg = bbApi )
                    false
                  }

                // Если API выключено, нужно убедится, что разрешено включать.
                if {
                  val r = isEnabled0 || askEnableBt
                  if (!r) logger.log( ErrorMsgs.BLE_BT_DISABLED, msg = (bbApi, isEnabled0, askEnableBt) )
                  r
                }

                // Если bt вЫключен, то запустить активацию bt:
                isEnabled2 <- {
                  if (isEnabled0)
                    Future.successful(isEnabled0)
                  else
                    bbApi
                      .enableBle()
                      // iOS: Ошибку включения трактуем как успех: пусть ошибка возникнет на уровне listenBeacons().
                      .recover { case ex =>
                        logger.info( ErrorMsgs.BLE_BEACONS_API_ENABLE_FAILED, ex, msg = bbApi )
                        true
                      }
                }

                // Если bt точно выключен, то смысла запускать сканирование нет.
                if {
                  val r = isEnabled2
                  if (!r)
                    logger.log( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, msg = (bbApi, r) )
                  r
                }

                // Запустить непосредственное слушанье маячков:
                _ <- bbApi.listenBeacons( dispatcher(_: BeaconDetected) )

              } yield {
                bbApi
              }
            }
            .recoverWith { case ex: Throwable =>
              logger.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, ex, bbApi )

              // На всякий случай - в фоне постараться грохнуть это API.
              // Далее - велосипед для безопасного опускания неисправного API и переходу на следующий шаг:
              val nextP = Promise[None.type]()
              val runNextFut = nextP.future.flatMap { _ =>
                // перейти к следующему api:
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
              // Запустить таймер макс.ожидания опускания неисправного API.
              DomQuick.setTimeout(2000)(__runNext)

              runNextFut
            }
        }
      }

      // И запустить цикл асинхронной свёрстки списка доступных API, чтобы найти первое удачное рабочее API:
      __foldApisAsync( IBleBeaconsApi.detectApis() )
        .transform { tryRes =>
          Success( HandleListenRes( tryRes ) )
        }
    }
  }


  /** Автоматическое управление интервалом gc для самоочистки списка маячков.
    * Можно вызывать часто и много раз, главное правильные данные на вход подавать.
    * Метод имеет сайд-эффекты в виде запуска/остановки таймера.
    *
    * @param beaconsEmpty Есть ли маячки в карте маячков? true - если нет.
    *                     Если карта пустая, то таймер будет остановлен.
    *                     Иначе - запущен.
    * @param gcIntervalOpt Данные текущего запущенного gc-таймера, если есть.
    *
    * @return Обновлённые (или те же) данные gc-таймера.
    */
  def ensureGcInterval(beaconsEmpty: Boolean, gcIntervalOpt: Option[Int]): Option[Int] = {
    gcIntervalOpt.fold {
      // Таймер сейчас не запущен.
      if (beaconsEmpty) {
        gcIntervalOpt
      } else {
        // Есть маячки, таймер не запущен. Запустить новый таймер.
        val ivlId = DomQuick.setInterval( BleBeaconerAh.GC_LOST_EVERY_MS ) { () =>
          dispatcher.dispatch( DoGc )
        }
        Some(ivlId)
      }

    } { intervalId =>
      // Уже запущен gc-таймер. Проверить, актуален ли он сейчас?
      if (beaconsEmpty) {
        // Нет смысла в gc-таймере: нечего чистить.
        DomQuick.clearInterval( intervalId )
        None
      } else {
        // Есть запущенный gc-таймер, есть маячки для gc. Продолжаем без изменений.
        gcIntervalOpt
      }
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // API сообщает, что получило сигнал от какого-то ble-маячка.
    case m: BeaconDetected =>
      val v0 = value

      if (v0.isEnabled contains[Boolean] false) {
        logger.info( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } else {
        // Ожидаемый сигнал с маячков. Обработать сообщение.
        // Есть состояние beaconer'а, как и ожидалось. Надо залить данные с маячка в состояние.
        //LOG.log( "hBS", msg = m )

        m.signal.beaconUid
          .filter(_.nonEmpty)
          // Интересуют только маячки с идентификаторами.
          .fold {
            logger.warn(ErrorMsgs.BLE_BEACON_EMPTY_UID, msg = m)
            noChange
          } { bUid =>
            val distanceOptM = RadioUtil.calculateAccuracy(m.signal)

            val distanceM: Double = distanceOptM getOrElse {
              logger.log(ErrorMsgs.BEACON_ACCURACY_UNKNOWN, msg = m.signal)
              99
            }
            // Нам проще работать с целочисленной дистанцией в сантиметрах
            val distanceCm = (distanceM * 100).toInt

            //LOG.log( msg = (bUid :: distanceCm :: Nil).mkString(" ") )

            val beaconDataOpt0 = v0.beacons.get( bUid )
            val accuracies2 = beaconDataOpt0
              .fold( RunningAverage[Int](6, knownLen = Some(0)) )( _.accuracies )
              .push( distanceCm )

            val beaconData1 = beaconDataOpt0
              .fold( MBeaconData.apply _ )( _.copy )
              .apply( m, accuracies2 )

            val beacons2 = v0.beacons + ((bUid, beaconData1))

            // Если сейчас нет таймера уведомления системы, то надо запустить его.
            val (notifyAllTimerOpt2, notifyFxOpt) = BleBeaconerAh.ensureNotifyAllDirtyTimer( v0.notifyAllTimer, beacons2, v0.envFingerPrint )

            // Убедиться, что gc-таймер сейчас запущен:
            val gcIvl2 = ensureGcInterval( beacons2.isEmpty, v0.gcIntervalId )

            val v2 = v0.copy(
              notifyAllTimer  = notifyAllTimerOpt2,
              beacons         = beacons2,
              gcIntervalId    = gcIvl2
            )
            ah.optionalResult( Some(v2), notifyFxOpt, silent = bcnsIsSilentRO.value )
          }
      }


    // Команда к запуску чистки списка маячков от неактуальных данных.
    case DoGc =>
      val v0 = value
      // v0.beacons.isEmpty можно не проверять, но проверяем на всякий случай.
      if (v0.beacons.isEmpty) {
        val v2 = MBeaconerS.gcIntervalId
          .modify( ensureGcInterval(v0.beacons.isEmpty, _) )(v0)
        updated(v2)

      } else {
        val ttl = BleBeaconerAh.FORGET_UNSEEN_AFTER
        val now = System.currentTimeMillis()

        // Собрать id маячков, подлежащий удалению.
        val keys2delete = (for {
          (k, mbd) <- v0.beacons.iterator
          if {
            val isOk = now - mbd.detect.seenAtMs < ttl
            val isToDelete = !isOk
            isToDelete
          }
        } yield k)
          .toSet

        if (keys2delete.isEmpty) {
          // Нет ничего для удаления из карты.
          noChange
        } else {
          // Есть маячки, которые требуют удаления из основной карты. Для снижения нагрузки на CPU делаем view, а ребилд всей карты будет потом.
          val beacons2 = v0.beacons
            .view
            .filterKeys( !keys2delete.contains(_) )
            .toMap

          val (notifyAllTimerOpt2, notifyFxOpt) = BleBeaconerAh.ensureNotifyAllDirtyTimer( v0.notifyAllTimer, beacons2, v0.envFingerPrint )

          // Проверить, остались ли ещё маячки в списке. Если нет, то gc-таймер более не нужен.
          val gcIvl2 = if (keys2delete.size ==* v0.beacons.size) {
            // Кажется, что не осталось маячков. gc-таймер можно грохать.
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


    // Сработал таймер уведомления в внешней системы о существенном изменении среди маячков.
    case m: MaybeNotifyAll =>
      val v0 = value

      def __maybeRmTimer() = {
        Option.when( v0.notifyAllTimer.nonEmpty ) {
          MBeaconerS.notifyAllTimer.set( None )(v0)
        }
      }

      if (v0.notifyAllTimer.exists(_.timestamp ==* m.timestamp)) {
        // Это ожидаемый таймер сработал. Пересчитать контрольную сумму маячков:
        val beaconsNearby = BleBeaconerAh.beaconsNearby( v0.beacons )
        val fpr2 = BleBeaconerAh.mkFingerPrint( beaconsNearby )

        def __onChangeFxOpt(v2: MBeaconerS) =
          onNearbyChange
            .flatMap( _(v0.nearbyReport, v2.nearbyReport) )

        if (fpr2 !=* v0.envFingerPrint) {
          // Изменился отпечаток маячков с момента последнего уведомления всех вокруг. Надо организовать обновлённый список маячков.
          val v2 = v0.copy(
            envFingerPrint = fpr2,
            notifyAllTimer = None,
            nearbyReport   = beaconsNearby.map(_._2),
          )
          // Опциональный onChange-эффект
          // silent, т.к. функция onChange вынесена в конструктор, и подписка на изменения не подразумевается.
          ah.updatedSilentMaybeEffect( v2, __onChangeFxOpt(v2) )
          // Без silent, т.к. обычно есть подписка на fingerPrint или nearbyReport.
          //updated(v2)

        } else {
          // Нет смысла уведомлять кого-либо: ничего существенно не изменилось в маячков.
          val v2Opt = __maybeRmTimer()
          // Если oneShot, то нужно уведомить через onChange, хотя ничего и не изменилось - функция сама порешит.
          val fxOpt = OptionUtil.maybeOpt( v0.opts.oneShot )(
            __onChangeFxOpt(v2Opt getOrElse v0)
          )
          this.optionalResult( v2Opt, fxOpt, silent = true )
        }

      } else {
        // Неожиданный таймер, игнор.
        logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = m )
        val v2 = __maybeRmTimer()
        v2.fold(noChange)(updatedSilent)
      }


    // Управление активностью BleBeaconer: вкл/выкл.
    case m: BtOnOff =>
      val v0 = value
      // Включён или включается.
      val isEnabledNow = v0.isEnabled contains[Boolean] true
      val isEnabled2 = m.isEnabled contains[Boolean] true
      def isDisabled2 = m.isEnabled contains[Boolean] false
      def hasBle2 = v0.hasBle orElse v0.hasBle.pending()

      if (
        // Сначала проверяем на предмет дублирующегося включения с изменением опций работы демона.
        // Здесь может быть pending в isEnabled -- это нормально, если дублирующийся сигнал запуска был слишком быстрым.
        // hardOff-флаг не проверяем, т.к. демон уже включён (isEnabledNow).
        isEnabled2 && isEnabledNow &&
        (m.opts !=* v0.opts)
      ) {
        // Обновление опций работы демона. Такое бывает, если BtOnOff был запущен сначала из контроллера демона,
        // а затем одновременно из Sc3Circuit при активации в приложения.
        val v2 = (MBeaconerS.opts set m.opts)(v0)
        updatedSilent(v2)

      } else if (v0.isEnabled.isPending) {
        // Отработать ситуацию с pending и повторным включением или выключением.
        if (!isEnabledNow && isDisabled2) {
          // Повторное выключение. Игнорить.
          logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.isEnabled) )
          noChange

        } else if (
          (isEnabledNow && isDisabled2) ||
          (!isEnabledNow && isEnabled2)
        ) {
          // Если BLE-мониторинг включается, а поступил сигнал ВЫключения, надо дождаться активации и выключиться.
          val v2 = (MBeaconerS.afterOnOff set Some(m.toEffectPure))(v0)
          updatedSilent(v2)

        } else {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled, v0.opts) )
          noChange
        }

      } else if (!isEnabledNow && isEnabled2) {
        // !isEnabledNow: Здесь учитывается также, что случай v0.isEnabled=Pot.empty - норма для первого включения.
        // Активировать BleBeaconer: запустить подписание на API.
        val apiActFx = startApiActivation(
          askEnableBt = v0.opts.askEnableBt,
        )
        // Эффект подписки на маячковое API:
        val v2 = v0.copy(
          isEnabled     = v0.isEnabled
            .ready(true)
            .pending(),
          bleBeaconsApi = v0.bleBeaconsApi.pending(),
          // По идее, тут всегда None. Но в теории возможно и что-то невероятное...
          gcIntervalId  = ensureGcInterval( v0.beacons.isEmpty, v0.gcIntervalId ),
          opts          = m.opts,
          hasBle        = hasBle2,
        )
        updated( v2, apiActFx )

      } else if (isEnabledNow && isDisabled2) {
        // Гасим таймеры в состоянии:
        for (timerInfo <- v0.notifyAllTimer)
          DomQuick.clearTimeout( timerInfo.timerId )

        // Гасим BleBeaconer: выключить API, грохнуть все таймеры в состоянии и т.д.
        val apiStopFxOpt = for {
          bbApi <- v0.bleBeaconsApi.toOption
        } yield {
          Effect {
            bbApi
              .unListenAllBeacons()
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

        // если hard-сброс, то очистить карту.
        val beacons2: Map[String, MBeaconData] = {
          Map.empty
        }
        val bcnsNearby2 = BleBeaconerAh.beaconsNearby( beacons2 )

        // Собрать новое состояние.
        val v2 = v0.copy(
          isEnabled         = v0.isEnabled
            .ready(false)
            .pending(),
          notifyAllTimer    = None,
          envFingerPrint    = BleBeaconerAh.mkFingerPrint( bcnsNearby2 ),
          bleBeaconsApi     = Pot.empty,
          // Надо грохнуть gc-таймер. Имитируем для этого естественный ход событий:
          gcIntervalId      = ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId),
          opts              = m.opts,
          nearbyReport      = bcnsNearby2.map(_._2),
          beacons           = beacons2,
          hasBle            = hasBle2,
        )

        ah.updatedMaybeEffect( v2, apiStopFxOpt )

      } else {
        // Уже включёно, или уже выключено, или m.isEnabled == None.
        // Но если hasBle.isEmpty, надо всё-таки посмотреть, что там с доступностью bluetooth вообще.
        if (v0.hasBle.isEmpty) {
          val hasBleFx = Effect.action {
            val hasBlePot2 = v0.hasBle withTry Try {
              IBleBeaconsApi
                .detectApis()
                .nonEmpty
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


    // Обработать результат подписки API на события.
    case m: HandleListenRes =>
      val v0 = value

      if (!v0.bleBeaconsApi.isPending) {
        // API выключено. Что-то не так, наверное.
        logger.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, msg = m )
        noChange

      } else {
        // Сейчас система включена, и API активно. Значит пришёл сигнал об активации API (или ошибке активации).
        // Запустить таймер перехода из тихого в нормальный режим работы (уведомлять всех).
        m.listenTryRes.fold(
          {ex =>
            val beacons2 = Map.empty[String, MBeaconData]
            val gcTimer2 = ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId)
            logger.error(ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, ex = ex, msg = m)
            // Какая-то ошибка активации API. Ошибочное API уже должно быть выключено само.
            val falsePot2 = v0.isEnabled.ready(false)
            val v2 = v0.copy(
              isEnabled         = falsePot2,
              bleBeaconsApi     = v0.bleBeaconsApi.fail(ex),
              notifyAllTimer    = None,
              beacons           = beacons2,
              gcIntervalId      = gcTimer2,
              afterOnOff        = None,
              hasBle = v0.hasBle
                .orElse( falsePot2 )
                .fail(ex),
            )
            ah.updatedMaybeEffect( v2, v0.afterOnOff )
          },
          {bbApi =>
            // Успешная активация API. Надо запустить таймер начального накопления данных по маячкам.
            val (timerInfo, timerFx) = BleBeaconerAh.startNotifyAllTimer( BleBeaconerAh.EARLY_INIT_TIMEOUT_MS )
            val truePot2 = v0.isEnabled ready true
            val v2 = v0.copy(
              notifyAllTimer    = Some(timerInfo),
              isEnabled         = truePot2,
              bleBeaconsApi     = v0.bleBeaconsApi.ready( bbApi ),
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
        )
      }


    // Сигнал окончания запуска или инициализации системы.
    case m: BtOnOffFinish =>
      val v0 = value

      def __maybeNoChange =
        v0.afterOnOff.fold( noChange ) { afterOnOffFx =>
          val v2 = (MBeaconerS.afterOnOff set None)(v0)
          updatedSilent(v2, afterOnOffFx)
        }

      if (!v0.isEnabled.isPending) {
        // Вообще, такого бывать не должно, чтобы pending слетал до ReadyEnabled
        logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = m )
        __maybeNoChange

      } else {
        // Система ожидает инициализации.
        var v2F = MBeaconerS.isEnabled
          .modify( _ withTry m.tryEnabled )

        if (v0.afterOnOff.nonEmpty)
          v2F = v2F andThen (MBeaconerS.afterOnOff set None)

        def __updatedMaybeEffect =
          ah.updatedMaybeEffect( v2F(v0), v0.afterOnOff )

        m.tryEnabled.fold(
          {_ =>
            // Ошибка проведения инициализации системы.
            __updatedMaybeEffect
          },
          {isEnabled2 =>
            if (v0.isEnabled contains[Boolean] isEnabled2) {
              // Завершена ожидавшаяся инициализация или де-инициализация.
              __updatedMaybeEffect
            } else {
              // Внезапно, сигнал о готовности мимо кассы: ожидается обратная готовность (включение вместо выключения и наоборот).
              logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled) )
              __maybeNoChange
            }
          }
        )
      }


    // Изменение состояния флага hasBle.
    case m: HasBleRes =>
      val v0 = value
      if (v0.hasBle ==* m.hasBle) {
        noChange

      } else {
        val v2 = (MBeaconerS.hasBle set m.hasBle)(v0)
        // По идее, подписки снаружи на это значение нет, поэтому тут silent.
        updatedSilent(v2)
      }

  }

}
