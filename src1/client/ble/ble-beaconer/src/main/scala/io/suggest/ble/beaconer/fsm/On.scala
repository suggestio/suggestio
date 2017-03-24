package io.suggest.ble.beaconer.fsm

import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m.BeaconSd
import io.suggest.ble.beaconer.m.signals.{BeaconDetected, BeaconsNearby}
import io.suggest.common.radio.{BeaconUtil, RadioUtil}
import io.suggest.sjs.common.ble.MBleBeaconInfo
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.signals.IVisibilityChangeSignal
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 12:25
  * Description: Поддержка режима мониторинга ble-маячков.
  */
trait On extends BeaconerFsmStub { thisFsm =>

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


  /** Интерфейс для поля _offlineState. */
  trait IOfflineState {

    /** Состояние пребывания в отключке. */
    def _offlineState: FsmState

    def _suspendedState: FsmState

  }


  /** Базовый трейт для состояний нахождения в активном состоянии поиска BLE-маячков.
    * Любое активное состояние слушает маячки и сохраняет их себе в состояние. */
  sealed trait ActiveStateBaseT extends FsmState with IOfflineState {

    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData
      if (sd0.listeningOn.isEmpty) {
        // Запустить мониторинг маячков.
        val apiOpt = IBleBeaconsApi.detectApi
        apiOpt.get
          .listenBeacons(thisFsm)

        // Сохранить в состояние API, на сообщения которого подписаны.
        _stateData = sd0.withListeningOn(apiOpt)
      }
    }

    override def receiverPart: Receive = {
      val r: Receive = {
        case bs: BeaconDetected =>
          _handleBeaconSeen(bs)
      }
      r.orElse( super.receiverPart )
    }


    /** Реакция на сигнал обнаружения маячка. */
    def _handleBeaconSeen(bs: BeaconDetected): Unit = {
      bs.beacon.uid
        .filter(_.nonEmpty)
        // Интересуют только маячки с идентификаторами.
        .fold[Unit] {
          LOG.log(WarnMsgs.BLE_BEACON_EMPTY_UID, msg = bs)
        } { bUid =>
          for {
            // для которых можно оценить мгновенное расстояние...
            distanceM <- {
              val dOpt = RadioUtil.calculateAccuracy(bs.beacon)
              if (dOpt.isEmpty)
                LOG.warn(WarnMsgs.BEACON_ACCURACY_UNKNOWN, msg = bs.beacon)
              dOpt
            }

          } {
            val sd0 = _stateData
            val beaconSd1 = sd0.beacons
              .get(bUid)
              .fold[BeaconSd] {
                BeaconSd(
                  beacon     = bs.beacon,
                  lastSeenMs = bs.seen
                )
              } { currBeaconSd =>
                currBeaconSd.copy(
                  beacon     = bs.beacon,
                  lastSeenMs = bs.seen
                )
              }
            // Нам проще работать с целочисленной дистанцией в сантиметрах
            val distanceCm = (distanceM * 100).toInt

            beaconSd1.accuracies.pushValue(distanceCm)
            _stateData = sd0.withBeacons(
              sd0.beacons + ((bUid, beaconSd1))
            )
          }
        }
    }


    /** Просто уйти в отключку немедленно. */
    def _goOffline(): Unit = {
      val sd0 = _stateData

      // Остановить мониторинг
      for (api <- sd0.listeningOn) {
        api.unListenAllBeacons()
      }

      // Обновить состояние.
      val sd1 = sd0.copy(
        beacons         = Map.empty,
        envFingerPrint  = None,
        listeningOn     = None
      )
      become( _offlineState, sd1 )
    }

    /** Реакция на сообщение об изменении visibility зависят от состояния. */
    override def _handleVisibilityChange(vc: IVisibilityChangeSignal): Unit = {
      if (vc.isHidden) {
        _goSuspend()
      } else {
        super._handleVisibilityChange(vc)
      }
    }

    /** Логика отправки в suspend-состояние. */
    def _goSuspend(): Unit = {
      val sd0 = _stateData
      // Остановить мониторинг BLE
      for (api <- sd0.listeningOn) {
        api.unListenAllBeacons()
      }
      val sd1 = sd0.withListeningOn(None)
      become(_suspendedState, sd1)
    }

  }



  /**
    * Ранняя стадия мониторинга маячков.
    * Мониторинг осуществляется, однако нужно молча накопить начальные данные,
    * прежде чем отчитываться о маячках.
    * Состояние заканчивается по срабатыванию таймера.
    */
  trait EarlyActiveStateT extends ActiveStateBaseT {

    private var _nextStateTimerId: Int = _

    case object EarlyTimeout

    override def afterBecome(): Unit = {
      try {
        // Запустить мониторинг маячков.
        super.afterBecome()

        // Запустить таймер окончания начальной инициализации состояния.
        _nextStateTimerId = DomQuick.setTimeout(EARLY_INIT_TIMEOUT_MS) { () =>
          _sendEventSync(EarlyTimeout)
        }

      } catch {
        case ex: Throwable =>
          LOG.error( ErrorMsgs.BLE_BEACONS_LISTEN_ERROR, ex )
          _goOffline()
      }
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      case EarlyTimeout =>
        _handleEarlyTimeout()
    }

    /** Реакция на срабатывания таймера окончания инициализации. */
    def _handleEarlyTimeout(): Unit = {
      become(_activeState)
    }


    /** Просто уйти в отключку немедленно. */
    override def _goOffline(): Unit = {
      DomQuick.clearTimeout( _nextStateTimerId )
      super._goOffline()
    }

    def _activeState: FsmState

  }



  /** Трейт состояния нормальой сборки маячков с рассылкой отчетов по ситуации. */
  trait ActiveStateT extends ActiveStateBaseT {

    /** id таймера самоочистки состояния. */
    private var _gcTimerId: Int = _

    /** Сигнал самому себе о необходимо выполнить самоочистку. */
    case object Gc


    /** Опциональный id таймера запуска _maybeNotifyWatchers(). */
    private var _maybeNotifyWatchersTimerId: Option[Int] = None

    case object MaybeNotifyWatchers


    /** Запустить мониторинг маячков. */
    override def afterBecome(): Unit = {
      super.afterBecome()

      // Отчитаться перед watcher'ами о текущих данных по обнаруженным маячкам.
      if (_stateData.beacons.nonEmpty) {
        _maybeNotifyWatchers()
      }

      // Запустить таймер самоочистки маячков, которые ушли из видимости какое-то время назад.
      _gcTimerId = DomQuick.setInterval(GC_LOST_EVERY_MS) { () =>
        _sendEventSync(Gc)
      }
    }


    override def receiverPart: Receive = super.receiverPart.orElse {
      case Gc =>
        _handleGc()

      case MaybeNotifyWatchers =>
        _maybeNotifyWatchersTimerId = None
        _maybeNotifyWatchers()
    }


    /** Реакция на сигнал сборки мусора. */
    def _handleGc(): Unit = {
      val sd0 = _stateData
      val ttl = FORGET_UNSEEN_AFTER
      val now = System.currentTimeMillis()
      val beacons2 = sd0.beacons.filter { case (_, v) =>
        now - v.lastSeenMs < ttl
      }
      // Если были какие-то изменения в карте маячков, то запланировать возможные уведомления для подписчиков.
      if (sd0.beacons.size != beacons2.size) {
        _stateData = sd0.withBeacons( beacons2 )
        _ensureNotifyWatchersTimer()
      }
    }


    // Выставить таймер вызова _maybeNotifyWatchers(). Пауза нужна для сглаживания паразитных рассылок.
    def _ensureNotifyWatchersTimer(): Unit = {
      if (_maybeNotifyWatchersTimerId.isEmpty) {
        val timerId = DomQuick.setTimeout(CHECK_BEACONS_DIRTY_AFTER_MS) { () =>
          _sendEventSync(MaybeNotifyWatchers)
        }
        _maybeNotifyWatchersTimerId = Some(timerId)
      }
    }

    /** Реакция на сигнал обнаружения маячка. */
    override def _handleBeaconSeen(bs: BeaconDetected): Unit = {
      super._handleBeaconSeen(bs)
      _ensureNotifyWatchersTimer()
    }


    /** Если изменился ли "отпечаток" маячков с момента последнего уведомления,
      * то уведомить всех смотрящих. */
    def _maybeNotifyWatchers(): Unit = {
      // Считаем отпечаток маячков: id по удалению, кроме совсем уж далёких.
      val sd0 = _stateData
      if (sd0.watchers.isEmpty)
        return

      // Выявляем только находящиеся рядом маячки.
      val beaconsNearby = {
        sd0.beacons
          .iterator
          .flatMap { case (k, v) =>
            for {
              accuracyCm    <- v.accuracies.average
              uid           <- v.beacon.uid
            } yield {
              (k, MBleBeaconInfo(uid, accuracyCm))
            }
          }
          //.filter { tuple =>
            // Маячки на значительном расстоянии уже неинтересны.
            // 2017.mar.24: Отключено. Тут стояло 20 почему-то, но в первом реальным магазине (ТК Гулливер) выяснилось
            // что из-за всяких разных факторов ощущаемые расстояния колеблятся ОТ 20 метров до 85 метров. А это значит,
            // что фильтровать по дальности вообще смысла нет, т.к. кроме проблем это ничего не сулит в реальной обстановке.
            //tuple._2.distanceCm < 10000
          //}
          .toSeq
      }

      // Считаем отпечаток: ближайшие маяки сортируем по id и вычисляем хэш результата.
      val fingerPrintNowOpt = if (beaconsNearby.isEmpty) {
        None
      } else {
        val hash = beaconsNearby
          .map { case (bcnUid, rep) =>
            bcnUid -> BeaconUtil.quantedDistance(rep.distanceCm)
          }
          // Сортируем по нормализованному кванту расстояния и по id маячка, чтобы раздавить флуктуации от рядом находящихся маячков.
          .sortBy { case (bcnUid, distQ) =>
            "%04d".format(distQ) + "." + bcnUid
          }
          .map(_._1)
          .hashCode()
        Some(hash)
      }

      if (sd0.envFingerPrint != fingerPrintNowOpt) {
        // Изменилось состояние маячкового окружения. Требуется сформировать отчёт по маячкам и уведомить watcher'ов.
        val signal = BeaconsNearby(
          beacons = beaconsNearby.map(_._2)
        )
        for (fsm <- sd0.watchers) {
          fsm ! signal
        }

        // Сохраняем новое значение отпечатка в состояние
        _stateData = sd0.withEnvFingerPrint( fingerPrintNowOpt )
      }
    }


    private def _clearTimers(): Unit = {
      // Остановить таймер GC.
      DomQuick.clearInterval( _gcTimerId )
      // Остановить возможный таймер проверки списка маячков.
      for (dirtyTimerId <- _maybeNotifyWatchersTimerId) {
        DomQuick.clearTimeout(dirtyTimerId)
      }
    }

    /** Уход в отключку требует некоторых действий. */
    override def _goOffline(): Unit = {
      _clearTimers()
      // Выполнить остальный действия по уходу в оффлайн.
      super._goOffline()
    }

    override def _goSuspend(): Unit = {
      _clearTimers()
      super._goSuspend()
    }
  }

}
