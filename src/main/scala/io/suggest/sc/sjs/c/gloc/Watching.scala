package io.suggest.sc.sjs.c.gloc

import io.suggest.sc.sjs.m.magent.VisibilityChange
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.vm.SafeWnd
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import org.scalajs.dom
import org.scalajs.dom.{Geolocation, Position, PositionError}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.04.16 11:28
  * Description: Аддон для [[GeoLocFsm]] для поддержки сборки состояния наблюдения за геолокацией.
  */
trait Watching extends GeoLocFsmStub {

  protected[this] def _startWatchersFor(wtypes: TraversableOnce[GlWatchType], glApi: Geolocation): Iterator[(GlWatchType, MglWatcher)] = {
    wtypes
      .toIterator
      .flatMap { wtype =>
        try {
          // Вешаем непрерывную слушалку событий геолокации.
          val wid = glApi.watchPosition(
            { p: Position =>
              val mgl = MGeoLoc(p)
              _sendEventSyncSafe( GlLocation(mgl, wtype) )
            }, { pe: PositionError =>
              _sendEventSync( GlError(pe, wtype) )
            },
            wtype.posOpts
          )
          val w = MglWatcher( Some(wid) )
          Seq(wtype -> w)

        } catch {
          case ex: Throwable =>
            error(ErrorMsgs.GEO_WATCH_TYPE_UNSUPPORTED, ex)
            Nil
        }
      }
  }

  /** Трейт для сборки состояния наблюдения за геолокацией пользователя. */
  trait WatchingStateT extends OffWhenNoSubscribersStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()

      for (glApi <- SafeWnd.geolocation) {
        // При переключение на состояние надо активировать watcher'ы геолокации.
        val sd0 = _stateData

        // Вычисляем недостающих watch'еров
        val needWatchers = GlWatchTypes.all -- sd0.watchers.keysIterator

        if (needWatchers.nonEmpty) {
          val watchersIter = _startWatchersFor(needWatchers, glApi)
          // Сохранить новых watcher'ов в состояние.
          _stateData = sd0.copy(
            watchers = {
              if (sd0.watchers.isEmpty)
                watchersIter.toMap
              else
                sd0.watchers ++ watchersIter
            }
          )
        }

      }
    }


    override def receiverPart: Receive = {
      // Сигнал от браузера о какой-то известной геолокации
      case loc: GlLocation =>
        _handleLocation(loc)
      // Сигнал от браузера об изменении visibility текущего документа.
      case vc: VisibilityChange =>
        _handleVisibilityChanged()

      // Сигнал срабатывания таймера подавления неточных геолокаций.
      case st: SuppressTimeout =>
        _handleSuppressTimeout(st)
      // Сигнал от браузера об ошибке геолокации.
      case err: GlError =>
        _handleLocationError(err)
    }

    /** Реакция на получение исзвестной геолокации. */
    def _handleLocation(loc: GlLocation): Unit = {
      println(loc)
      val sd0 = _stateData
      // TODO Проверять, изменились ли координаты. Десктопный Firefox шлёт одинаковые сообщения геолокации как по таймеру.

      // Собрать новые данные watcher'а
      val mgl1 = {
        val someLocData = Some(loc.data)
        sd0.watchers.get(loc.wtype).fold {
          warn( WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE + " " + loc.wtype )
          MglWatcher( lastPos = someLocData )
        } { mgl0 =>
          mgl0.copy( lastPos = someLocData )
        }
      }

      // !!! Из-за итераторов тут довольно взрывоопасный код, надо быть осторожнее.

      // Откладываем перестройку карты на последний момент: если нужен suppressor, то эта перестройка будет лишней.
      val watchers1Iter = sd0.watchers.iterator

      // Не подпадает ли текущая геолокация под нож подавления?
      val isLocNotSuppressed = _stateData.suppressor.fold(true) { s =>
        loc.wtype.precision >= s.minWatch.precision
      }

      if (isLocNotSuppressed) {
        // Если приходят высокоточные данные GPS, то надо отказаться от неточных данных геолокации хотя бы на какое-то время.
        val supprOpt = for {
          ttlMs <- loc.wtype.suppressorTtlMs
        } yield {

          // Отменить старый таймер подавления
          for (s <- sd0.suppressor) {
            DomQuick.clearTimeout(s.timerId)
          }

          // Найти и отрубить гео-watcher'ы, которые теперь подавляются.
          val (ws4cleanIter, wsKeepIter) = watchers1Iter.partition {
            case (wtype, _) =>
              wtype.precision <= loc.wtype.precision
          }
          val watchers2Iter = _clearWatchers(ws4cleanIter) ++ wsKeepIter

          // Запустить новый таймер подавления
          val generation = System.currentTimeMillis()
          val timerId = DomQuick.setTimeout(ttlMs) { () =>
            _sendEventSync( SuppressTimeout(generation) )
          }

          // Собрать и вернуть данные состояния
          val suppr = Suppressor(
            timerId     = timerId,
            generation  = generation,
            minWatch    = loc.wtype
          )
          (suppr, watchers2Iter)
        }

        // Полученный хардкорный итератор гео-вотчеров дополняется ещё и обновлёнными данными текущей геолокации.
        val wathersIter3 = supprOpt.fold(watchers1Iter)(_._2)  ++  Iterator(loc.wtype -> mgl1)

        // Сохранить новые данные в состояние.
        _stateData = sd0.copy(
          watchers    = wathersIter3.toMap,
          suppressor  = supprOpt.map(_._1)
        )

        // Уведомить subscriber'ов о новой локации. Режем уведомления от подавляемых гео-вотчеров
        _notifySubscribers(loc)

      } else {
        // Данная геолокация подпадает под подавление. Скорее всего что-то пошло не так.
        warn(WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE + " " + loc + " " + _stateData.suppressor)
        _stateData = sd0.copy(
          watchers = watchers1Iter.toMap
        )
      }
    }

    /** Реакция на ошибку геолокации. */
    def _handleLocationError(err: GlError): Unit = {
      error( ErrorMsgs.GEO_LOC_FAILED + " " + err.wtype + " " + err.error.code + " " + err.error.message )
      _notifySubscribers(err)
    }


    override def _beforeOffline(): Unit = {
      super._beforeOffline()
      // Наверное suppress больше не важен, отменить его TTL
      val sd0 = _stateData
      for (s <- sd0.suppressor) {
        DomQuick.clearTimeout( s.timerId )
        _stateData = sd0.copy(
          suppressor = None
        )
      }
    }

    /** Реакция на изменение видимости текущей страницы. Если страница сокрыта -- пора спать. */
    def _handleVisibilityChanged(): Unit = {
      if (dom.document.hidden) {
        _beforeOffline()
        become(_sleepingState)
      }
    }

    /** Реакция на срабатывания таймера подавления неточной геолокации. */
    def _handleSuppressTimeout(st: SuppressTimeout): Unit = {
      val sd0 = _stateData
      for {
        s     <- sd0.suppressor
        if s.generation == st.generation
        glApi <- SafeWnd.geolocation
      } {
        val watches1Iter = _startWatchersFor(
          wtypes  = s.minWatch.allPrevious.iterator,
          glApi   = glApi
        )
        _stateData = sd0.copy(
          watchers    = sd0.watchers ++ watches1Iter,
          suppressor  = None
        )
      }
    }

    /** Состояние временного сна геолокации. */
    def _sleepingState: FsmState

  }

}
