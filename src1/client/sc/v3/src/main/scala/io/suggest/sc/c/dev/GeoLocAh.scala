package io.suggest.sc.c.dev

import diode.data.Ready
import diode._
import io.suggest.geo.{GeoLocType, GeoLocTypes, MGeoLocJs, PositionException}
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sc.m.dev.{MGeoLocWatcher, MScGeoLoc, Suppressor}
import io.suggest.sc.m._
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.wnd.WindowVm
import japgolly.univeq._
import org.scalajs.dom.{Geolocation, Position, PositionError, PositionOptions}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.17 16:46
  * Description: Контроллер геолокации.
  * Работает в фоне, обновляя состояние, поэтому везде тут updatedSilent().
  */
class GeoLocAh[M](
                   dispatcher  : Dispatcher,
                   modelRW     : ModelRW[M, MScGeoLoc]
                 )
  extends ActionHandler( modelRW )
  with Log
{

  private def _geoLocApiOpt = WindowVm().geolocation

  /** Доп.операции для списков "наблюдателей" за геолокацией. */
  implicit class WatchersExtOps(val watchers: TraversableOnce[(GeoLocType, MGeoLocWatcher)]) {

    def clearWatchers(): Iterator[(GeoLocType, MGeoLocWatcher)] = {
      for {
        glApi         <- _geoLocApiOpt.iterator
        (wtype, w1)   <- watchers.toIterator
        w2 = w1.watchId.fold(w1) { watchId =>
          glApi.clearWatch(watchId)
          w1.copy(watchId = None)
        }
        // Если новый MglWatch пустой получился, то отбросить его.
        if w2.nonEmpty
      } yield {
        wtype -> w2
      }
    }

  }


  /** Доп.операции для списка типов наблюдателей за геолокацией. */
  implicit class GeoLocTypesOps(val wTypes: TraversableOnce[GeoLocType]) {

    def startWatchers(glApi: Geolocation): Iterator[(GeoLocType, MGeoLocWatcher)] = {
      wTypes
        .toIterator
        .flatMap { wtype =>
          try {
            val posOpts = js.Dictionary.empty[js.Any]
              .asInstanceOf[PositionOptions]
            posOpts.enableHighAccuracy = wtype.highAccuracy

            // Вешаем непрерывную слушалку событий геолокации.
            val wid = glApi.watchPosition(
              { p: Position =>
                val mgl = MGeoLocJs(p)
                dispatcher( GlLocation(wtype, mgl) )
              }, { pe: PositionError =>
                dispatcher( GlError(wtype, PositionException(pe)) )
              },
              posOpts
            )
            val w = MGeoLocWatcher( Some(wid) )
            (wtype -> w) :: Nil

          } catch {
            case ex: Throwable =>
              LOG.error(ErrorMsgs.GEO_WATCH_TYPE_UNSUPPORTED, ex)
              Nil
          }
        }
    }

  }

  private def _isLocNotSuppressed(glType: GeoLocType, v0: MScGeoLoc): Boolean = {
    v0.suppressor.fold(true) { s =>
      glType.precision >= s.minWatch.precision
    }
  }


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Есть на руках местоположение.
    case loc: GlLocation =>
      //println(loc)
      val v0 = value

      // Собрать новые данные watcher'а
      val mgl1 = v0.watchers
        .get( loc.glType )
        .fold {
          LOG.warn( WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = loc.glType.toString )
          MGeoLocWatcher( watchId = None, lastPos = Ready(loc.location) )
        } { mgl0 =>
          mgl0.withLastPos(
            lastPos = mgl0.lastPos.ready( loc.location )
          )
        }

      // !!! Из-за итераторов тут довольно взрывоопасный код, надо быть осторожнее.

      // Откладываем перестройку карты на последний момент: если нужен suppressor, то эта перестройка будет лишней.
      val watchers1Iter = v0.watchers.iterator

      // Не подпадает ли текущая геолокация под нож подавления?
      if ( _isLocNotSuppressed(loc.glType, v0) ) {
        // Если приходят высокоточные данные GPS, то надо отказаться от неточных данных геолокации хотя бы на какое-то время.
        val supprOpt = for {
          ttlMs <- loc.glType.suppressorTtlMs
        } yield {
          // Отменить старый таймер подавления
          for (s <- v0.suppressor) {
            DomQuick.clearTimeout(s.timerId)
          }

          // Найти и отрубить гео-watcher'ы, которые теперь подавляются.
          val (ws4cleanIter, wsKeepIter) = watchers1Iter.partition {
            case (wtype, _) =>
              wtype.precision <= loc.glType.precision
          }
          val watchers2Iter = ws4cleanIter.clearWatchers() ++ wsKeepIter

          // Запустить новый таймер подавления
          val generation = System.currentTimeMillis()
          // Здесь можно сделать эффект поверх future, заместо прямого дёрганья dispatcher'а.
          val timerId = DomQuick.setTimeout(ttlMs) { () =>
            dispatcher( GlSuppressTimeout(generation) )
          }

          // Собрать и вернуть данные состояния
          val suppr = Suppressor(
            timerId     = timerId,
            generation  = generation,
            minWatch    = loc.glType
          )
          (suppr, watchers2Iter)
        }

        // Полученный хардкорный итератор гео-вотчеров дополняется ещё и обновлёнными данными текущей геолокации.
        val watchers3 = {
          supprOpt.fold(watchers1Iter)(_._2)  ++  List(loc.glType -> mgl1)
        }.toMap

        // Сохранить новые данные в состояние.
        val v2 = v0.copy(
          watchers   = watchers3,
          suppressor = supprOpt.map(_._1)
        )

        // Уведомить другие контроллеры о наступлении геолокации.
        val notifyOthersFx = Effect.action( GlPubSignal(loc) )

        updatedSilent( v2, notifyOthersFx )

        // Уведомить subscriber'ов о новой локации.
        //_notifySubscribers(loc)

      } else {
        // Данная геолокация подпадает под подавление. Скорее всего что-то пошло не так.
        LOG.warn(WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = (loc, v0.suppressor))
        val v2 = v0.withWatchers(
          watchers1Iter.toMap
        )
        updatedSilent( v2 )
      }


    // Активация/деактивация геолокации.
    case m: GeoLocOnOff =>
      val v0 = value
      if (m.enabled ==* v0.isEnabled) {
        // Система уже активна, делать ничего не требуется.
        noChange

      } else {
        if (m.enabled) {
          _geoLocApiOpt.fold {
            LOG.warn( ErrorMsgs.GEO_LOC_FAILED, msg = m )
            noChange
          } { geoApi =>
            // Запустить мониторинг геолокации.
            val needWatchers = GeoLocTypes.all

            val watchers2 = needWatchers.startWatchers(geoApi).toMap
            // Сохранить новых watcher'ов в состояние.
            val v2 = v0.withWatchers(
              watchers2
            )
            updatedSilent(v2)
          }

        } else {
          // Заглушить геолокацию и suppressor.
          for (s <- v0.suppressor)
            DomQuick.clearTimeout(s.timerId)

          val v2 = v0.copy(
            watchers = v0.watchers
              .clearWatchers()
              .toMap,
            // Убрать suppressor, если активен.
            suppressor = None
          )

          updatedSilent(v2)
        }

      }


    // Сработал таймер окончания подавления suppressor'ом.
    case m: GlSuppressTimeout =>
      val v0 = value

      val resOpt = for {
        s     <- v0.suppressor
        if s.generation ==* m.generation
        glApi <- _geoLocApiOpt
      } yield {
        val watches1Iter = s.minWatch
          .allPrevious
          .iterator
          .startWatchers( glApi )

        val v2 = v0.copy(
          watchers    = v0.watchers ++ watches1Iter,
          suppressor  = None
        )
        updatedSilent( v2 )
      }

      resOpt.getOrElse {
        LOG.warn( WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
        noChange
      }


    // Ошибка считывания геолокации.
    case m: GlError =>
      LOG.error( ErrorMsgs.GEO_LOC_FAILED, msg = m )
      val v0 = value
      // Подхватываем ошибку, если есть куда записывать её.
      // Игнорим _isLocNotSuppressed(), т.к. ошибка не ломает возможное значение внутри Pot.
      v0.watchers
        .get( m.glType )
        .fold {
          // Should never happen.
          LOG.error( WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
          noChange
        } { watcher0 =>
          LOG.warn( ErrorMsgs.GEO_LOC_FAILED, msg = m )
          // Сохранить ошибку в lastLoc
          val wa2 = watcher0.withLastPos(
            watcher0.lastPos.fail( m.error )
          )
          val v2 = v0.withWatchers(
            v0.watchers + (m.glType -> wa2)
          )
          val notifyOthersFx = Effect.action( GlPubSignal(m) )
          updatedSilent(v2, notifyOthersFx)
        }

  }

}
