package io.suggest.ble.beaconer.c

import diode._
import diode.data.Pot
import io.suggest.ble.{BeaconUtil, MUidBeacon}
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m._
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.radio.RadioUtil
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.model.MTsTimerId
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick

import scala.concurrent.{Future, Promise}
import scala.util.Success

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
    */
  private def FORGET_UNSEEN_AFTER = 9000

  /**
    * Через сколько ms после получения уведомления от маячка, запускать проверку карты маячков на предмет
    * поиска измений окружающих маячков?
    * Таймер нужен, чтобы сгладить возможные флуктуации при высокой плотности маячков
    * и снизить нагрузку на устройства.
    */
  private def CHECK_BEACONS_DIRTY_AFTER_MS = 300


  /** Запуск поиска и активации Ble Beacon API.
    *
    * @param dispatcher Диспетчер экшенов, который подписывается на события обнаружения маячков.
    * @return Опциональный эффект.
    */
  def startApiActivation(dispatcher: Dispatcher): Option[Effect] = {
    // Поиск и подключение всех доступных API для сбора маячков.
    val apis = IBleBeaconsApi.detectApis()
    //LOG.log( msg = apis.mkString( HtmlConstants.COMMA ) )

    OptionUtil.maybe( apis.nonEmpty ) {
      // Подписаться на первое доступное API. При ошибках - переходить к следующему API по списку.
      // На все API нет смысла подписываться: тогда будут приходить ненужные уведомления.
      Effect {
        // Асинхронная свёрстка списка доступных API'шек.
        def __foldApisAsync(restApis: Seq[IBleBeaconsApi]): Future[IBleBeaconsApi] = {
          restApis.headOption.fold [Future[IBleBeaconsApi]] {
            val emsg = ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE
            LOG.log( emsg, msg = apis )
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
                      LOG.warn( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, ex, msg = bbApi )
                      false
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
                          LOG.warn( ErrorMsgs.BLE_BEACONS_API_ENABLE_FAILED, ex, msg = bbApi )
                          true
                        }
                  }
                  // Если bt точно выключен, то смысла запускать сканирование нет.
                  if {
                    val r = isEnabled2
                    if (!r)
                      LOG.log( ErrorMsgs.BLE_BEACONS_API_CHECK_ENABLED_FAILED, msg = (bbApi, r) )
                    r
                  }

                  // Запустить непосредственное слушанье маячков:
                  _ <- bbApi.listenBeacons(dispatcher.dispatch(_: BeaconDetected))

                } yield {
                  bbApi
                }
              }
              .recoverWith { case ex: Throwable =>
                LOG.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, ex, bbApi )

                // На всякий случай - в фоне постараться грохнуть это API.
                // Далее - велосипед для безопасного опускания неисправного API и переходу на следующий шаг:
                val nextP = Promise[None.type]()
                val runNextFut = nextP.future.flatMap { _ =>
                  // перейти к следующему api:
                  __foldApisAsync( restApis.tail )
                }

                def __runNext() =
                  if (!nextP.isCompleted)
                    nextP.success( None )

                try {
                  bbApi
                    .unListenAllBeacons()
                    .recover { case ex2 =>
                      LOG.error( ErrorMsgs.BLE_BEACONS_API_SHUTDOWN_FAILED, ex2, bbApi)
                      null
                    }
                    .foreach { _ =>
                      __runNext()
                    }
                } catch {
                  case ex3: Throwable =>
                    LOG.error( ErrorMsgs.BLE_BEACONS_API_SHUTDOWN_FAILED, ex3, bbApi)
                    __runNext()
                }
                // Запустить таймер макс.ожидания опускания неисправного API.
                DomQuick.setTimeout(2000)(__runNext)

                runNextFut
              }
          }
        }
        // И запустить цикл асинхронной свёрстки списка доступных API, чтобы найти первое удачное рабочее API:
        __foldApisAsync(apis)
          .transform { tryRes =>
            Success( HandleListenRes( tryRes ) )
          }
      }
    }
  }


  /** Собрать маячки поблизости.
    *
    * @param beacons Текущий список маячков.
    * @return Список близких маячков в неопределённом порядке.
    */
  def beaconsNearby(beacons: Iterable[(String, MBeaconData)]) : Seq[(String, MUidBeacon)] = {
    beacons
      .iterator
      .flatMap { case (k, v) =>
        val res = for {
          accuracyCm    <- v.accuracies.average
          uid           <- v.beacon.beaconUid
        } yield {
          (k, MUidBeacon(uid, accuracyCm))
        }
        if (res.isEmpty)
          LOG.warn( WarnMsgs.BEACON_ACCURACY_UNKNOWN, msg = v )
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
      beacons
        .map { case (bcnUid, rep) =>
          bcnUid -> BeaconUtil.quantedDistance(rep.distanceCm)
        }
        // Сортируем по нормализованному кванту расстояния и по id маячка, чтобы раздавить флуктуации от рядом находящихся маячков.
        .sortBy { case (bcnUid, distQ) =>
          "%04d".format(distQ) + "." + bcnUid
        }
        .map(_._1)
        .hashCode()
    }
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


class BleBeaconerAh[M](
                        dispatcher  : Dispatcher,
                        modelRW     : ModelRW[M, MBeaconerS]
                      )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

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
      //println(m)
      val v0 = value

      if (v0.isEnabled contains false) {
        LOG.info( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } else {
        // Ожидаемый сигнал с маячков. Обработать сообщение.
        // Есть состояние beaconer'а, как и ожидалось. Надо залить данные с маячка в состояние.
        //LOG.log( "hBS", msg = m )

        m.beacon.beaconUid
          .filter(_.nonEmpty)
          // Интересуют только маячки с идентификаторами.
          .fold {
            LOG.warn(WarnMsgs.BLE_BEACON_EMPTY_UID, msg = m)
            noChange
          } { bUid =>
            val distanceOptM = RadioUtil.calculateAccuracy(m.beacon)

            val beaconSd1 = v0.beacons
              .get(bUid)
              .fold[MBeaconData] {
                MBeaconData(
                  beacon     = m.beacon,
                  lastSeenMs = m.seen
                )
              } { currBeaconSd =>
                currBeaconSd.copy(
                  beacon     = m.beacon,
                  lastSeenMs = m.seen
                )
              }

            val distanceM: Double = distanceOptM.getOrElse {
              LOG.warn(WarnMsgs.BEACON_ACCURACY_UNKNOWN, msg = m.beacon)
              99
            }
            // Нам проще работать с целочисленной дистанцией в сантиметрах
            val distanceCm = (distanceM * 100).toInt

            //LOG.log( msg = (bUid :: distanceCm :: Nil).mkString(" ") )

            beaconSd1.accuracies.pushValue(distanceCm)

            val beacons2 = v0.beacons + ((bUid, beaconSd1))

            // Если сейчас нет таймера уведомления системы, то надо запустить его.
            val (notifyAllTimerOpt2, notifyFxOpt) = BleBeaconerAh.ensureNotifyAllDirtyTimer( v0.notifyAllTimer, beacons2, v0.envFingerPrint )

            // Убедиться, что gc-таймер сейчас запущен:
            val gcIvl2 = ensureGcInterval( beacons2.isEmpty, v0.gcIntervalId )

            val v2 = v0.copy(
              notifyAllTimer  = notifyAllTimerOpt2,
              beacons         = beacons2,
              gcIntervalId    = gcIvl2
            )
            ah.updatedSilentMaybeEffect(v2, notifyFxOpt)
          }
      }


    // Команда к запуску чистки списка маячков от неактуальных данных.
    case DoGc =>
      val v0 = value
      // v0.beacons.isEmpty можно не проверять, но проверяем на всякий случай.
      if (v0.beacons.isEmpty) {
        val v2 = v0.withGcIntervalId( ensureGcInterval(v0.beacons.isEmpty, v0.gcIntervalId) )
        updated(v2)

      } else {
        val ttl = BleBeaconerAh.FORGET_UNSEEN_AFTER
        val now = System.currentTimeMillis()

        // Собрать id маячков, подлежащий удалению.
        val keys2delete = v0.beacons
          .iterator
          .filter { case (k, mbd) =>
            val isOk = now - mbd.lastSeenMs < ttl
            val isToDelete = !isOk
            //println(s"drop?$isToDelete $k, now=$now lastSeen=${mbd.lastSeenMs} diff=${now - mbd.lastSeenMs} max-ttl=$ttl isOk=$isOk")
            isToDelete
          }
          .map(_._1)
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
          //println(s"gc: ${keys2delete.mkString(", ")} ${v0.beacons.size}->${beacons2.size}")

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
      //println(m)
      val v0 = value

      def __maybeRmTimer() = {
        if (v0.notifyAllTimer.nonEmpty) {
          val v2 = v0.withNotifyAllTimerId(None)
          updatedSilent(v2)
        } else {
          noChange
        }
      }

      if (v0.notifyAllTimer.exists(_.timestamp ==* m.timestamp)) {
        // Это ожидаемый таймер сработал. Пересчитать контрольную сумму маячков:
        val beaconsNearby = BleBeaconerAh.beaconsNearby( v0.beacons )
        val fpr2 = BleBeaconerAh.mkFingerPrint( beaconsNearby )
        if (fpr2 !=* v0.envFingerPrint) {
          //println(s"fpr CHANGED: ${v0.envFingerPrint}=>$fpr2\n beaconsMap = ${v0.beacons}\n nearby = ${beaconsNearby.mkString(", ")}")
          // Изменился отпечаток маячков с момента последнего уведомления всех вокруг. Надо организовать обновлённый список маячков.
          val v2 = v0.copy(
            envFingerPrint = fpr2,
            notifyAllTimer = None,
            nearbyReport   = beaconsNearby.map(_._2)
          )
          // Без silent, т.к. обычно есть подписка на fingerPrint или nearbyReport.
          updated(v2)

        } else {
          // Нет смысла уведомлять кого-либо: ничего существенно не изменилось в маячков.
          //println("nothing changed " + v0.envFingerPrint + "==" + fpr2)
          __maybeRmTimer()
        }

      } else {
        // Неожиданный таймер, игнор.
        LOG.log( WarnMsgs.INACTUAL_NOTIFICATION, msg = m )
        __maybeRmTimer()
      }


    // Управление активностью BleBeaconer: вкл/выкл.
    case m: BtOnOff =>
      val v0 = value
      // ! contains true вместо false, т.к. тут обычен случай Pot.empty.
      if (!v0.isEnabled.contains(true) && m.isEnabled) {
        val isCanBeEnabled = m.hard || !v0.hardOff
        // Активировать BleBeaconer: запустить подписание на API.
        if (!isCanBeEnabled) {
          LOG.log( WarnMsgs.SUPPRESSED_INSUFFICIENT, msg = (m, v0.isEnabled, v0.hardOff) )
          noChange

        } else {
          //println("bb ON @" + System.currentTimeMillis())
          val resOpt = for {
            // TODO Запускать асинхронную проверку isEnabled(), и/или вызов enable() и т.д.
            apiActFx <- BleBeaconerAh.startApiActivation( dispatcher )
          } yield {
            // Эффект подписки на маячковое API:
            val v2 = v0.copy(
              isEnabled     = v0.isEnabled.ready(true).pending(),
              bleBeaconsApi = v0.bleBeaconsApi.pending(),
              // По идее, тут всегда None. Но в теории возможно и что-то невероятное...
              gcIntervalId  = ensureGcInterval( v0.beacons.isEmpty, v0.gcIntervalId ),
              hardOff       = false
            )
            updated( v2, apiActFx )
          }
          resOpt.getOrElse {
            LOG.log(ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE, msg = m)
            noChange
          }
        }

      } else if (v0.isEnabled.contains(true) && !m.isEnabled) {
        // Гасим таймеры в состоянии:
        for (timerInfo <- v0.notifyAllTimer)
          DomQuick.clearTimeout( timerInfo.timerId )

        // Гасим BleBeaconer: выключить API, грохнуть все таймеры в состоянии и т.д.
        val apiStopFxOpt = for (bbApi <- v0.bleBeaconsApi.toOption) yield {
          Effect {
            bbApi
              .unListenAllBeacons()
              .transform { tryRes =>
                for (ex <- tryRes.failed)
                  LOG.error( ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE, ex, m )
                val action = ReadyEnabled(
                  tryEnabled = tryRes.map(_ => false)
                )
                Success(action)
              }
          }
        }

        // если hard-сброс, то очистить карту.
        val beacons2: Map[String, MBeaconData] =
          if (m.hard) Map.empty else v0.beacons

        // Собрать новое состояние.
        val v2 = v0.copy(
          isEnabled         = v0.isEnabled.ready(false).pending(),
          notifyAllTimer    = None,
          envFingerPrint    = None,
          bleBeaconsApi     = Pot.empty,
          // Надо грохнуть gc-таймер. Имитируем для этого естественный ход событий:
          gcIntervalId      = ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId),
          hardOff           = m.hard,
          nearbyReport      = if (beacons2.isEmpty) Nil else v0.nearbyReport,
          beacons           = beacons2
        )

        ah.updatedMaybeEffect( v2, apiStopFxOpt )

      } else {
        // Уже включёно или уже выключено.
        LOG.log( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled) )
        noChange
      }


    // Обработать результат подписки API на события.
    case m: HandleListenRes =>
      val v0 = value
      if (!v0.bleBeaconsApi.isPending) {
        // API выключено. Что-то не так, наверное.
        LOG.error( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, msg = m )
        noChange
      } else {
        // Сейчас система включена, и API активно. Значит пришёл сигнал об активации API (или ошибке активации).
        // Запустить таймер перехода из тихого в нормальный режим работы (уведомлять всех).
        m.listenTryRes.fold(
          {ex =>
            val beacons2 = Map.empty[String, MBeaconData]
            val gcTimer2 = ensureGcInterval(beacons2.isEmpty, v0.gcIntervalId)
            LOG.error(ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED, ex = ex, msg = m)
            // Какая-то ошибка активации API. Ошибочное API уже должно быть выключено само.
            val v2 = v0.copy(
              isEnabled         = v0.isEnabled.ready(false),
              bleBeaconsApi     = v0.bleBeaconsApi.fail(ex),
              notifyAllTimer    = None,
              beacons           = beacons2,
              gcIntervalId      = gcTimer2
            )
            updated(v2)
          },
          {bbApi =>
            // Успешная активация API. Надо запустить таймер начального накопления данных по маячкам.
            //println("bb API active @" + System.currentTimeMillis())
            val (timerInfo, fx) = BleBeaconerAh.startNotifyAllTimer( BleBeaconerAh.EARLY_INIT_TIMEOUT_MS )
            val v2 = v0.copy(
              notifyAllTimer    = Some(timerInfo),
              isEnabled         = v0.isEnabled.ready(true),
              bleBeaconsApi     = v0.bleBeaconsApi.ready( bbApi ),
              gcIntervalId      = ensureGcInterval(v0.beacons.isEmpty, v0.gcIntervalId)
            )
            updated(v2, fx)
          }
        )
      }


    // Сигнал окончания запуска или инициализации системы.
    case m: ReadyEnabled =>
      val v0 = value
      if (!v0.isEnabled.isPending) {
        // Вообще, такого бывать не должно, чтобы pending слетал до ReadyEnabled
        LOG.log( WarnMsgs.INACTUAL_NOTIFICATION, msg = m )
        noChange
      } else {
        // Система ожидает инициализации.
        m.tryEnabled.fold(
          {ex =>
            // Ошибка проведения инициализации системы.
            val v2 = v0.withIsEnabled(
              v0.isEnabled.fail(ex)
            )
            updated( v2 )
          },
          {isEnabled2 =>
            if (v0.isEnabled.contains(isEnabled2)) {
              // Завершена ожидавшаяся инициализация или де-инициализация.
              val v2 = v0.withIsEnabled(
                v0.isEnabled.ready( isEnabled2 )
              )
              updated(v2)
            } else {
              // Внезапно, сигнал о готовности мимо кассы: ожидается обратная готовность (включение вместо выключения и наоборот).
              LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.isEnabled) )
              noChange
            }
          }
        )
      }

  }

}
