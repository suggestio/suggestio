package io.suggest.sc.c.dev

import diode.data.{FailedStale, Ready}
import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.geo._
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.dev.{MGeoLocWatcher, MScGeoLoc, Suppressor}
import io.suggest.sc.m._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.dom._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import org.scalajs.dom.{Geolocation, Position, PositionError}
import io.suggest.spa.DiodeUtil.Implicits._

import scala.concurrent.duration._

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
{ ah =>

  import GeoLocAh.WatchersExtOps

  /** Доп.операции для списка типов наблюдателей за геолокацией. */
  implicit class GeoLocTypesOps(val wTypes: IterableOnce[GeoLocType]) {

    def startWatchers(glApi: Geolocation): Iterator[(GeoLocType, MGeoLocWatcher)] = {
      // Время кэша, чтобы можно было задействовать старую геолокацию.
      val maxAgeMs = 2.minutes.toMillis.toDouble
      wTypes
        .iterator
        .flatMap { wtype =>
          try {
            val posOpts = new PositionOptions {
              override val enableHighAccuracy = wtype.isHighAccuracy
              override val maximumAge         = maxAgeMs
            }

            // Вешаем непрерывную слушалку событий геолокации.
            val wid = glApi.watchPosition2(
              { p: Position =>
                val mgl = MGeoLocJs(p)
                dispatcher( GlLocation(wtype, mgl) )
              },
              { pe: PositionError =>
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


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Есть на руках местоположение.
    case loc: GlLocation =>
      //println(loc)
      val v0 = value

      // Не подпадает ли текущая геолокация под нож подавления?
      if ( _isLocNotSuppressed(loc.glType, v0) ) {
        // Собрать новые данные watcher'а
        //val coordNorm = loc.location.point

        val mglw0 = v0.watchers.get( loc.glType )
        if ( mglw0.exists(_.lastPos contains loc.location) ) {
          // Данная точка уже была получена в прошлый раз, повторно уведомлять не требуется.
          noChange

        } else {

          val mglw1 = mglw0.fold {
            LOG.warn( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = loc.glType.toString )
            MGeoLocWatcher( watchId = None, lastPos = Ready(loc.location) )
          } { mgl0 =>
            mgl0.withLastPos(
              lastPos = mgl0.lastPos.ready( loc.location )
            )
          }

          // !!! Из-за итераторов тут довольно взрывоопасный код, надо быть осторожнее.

          // Откладываем перестройку карты на последний момент: если нужен suppressor, то эта перестройка будет лишней.
          val watchers1Iter = v0.watchers.iterator

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
            val tl = (loc.glType -> mglw1) :: Nil
            supprOpt.fold(watchers1Iter)(_._2)  ++  tl
          }.toMap

          // Если pending, то переставить в ready.
          val switch2 = if (v0.switch.onOff.isPending) {
            val isOnOff = v0.switch.onOff.getOrElse(true)
            v0.switch.copy(
              onOff    = v0.switch.onOff.ready( isOnOff ),
              scSwitch = None
            )
          } else {
            v0.switch.withOutScSwitch
          }

          // Сохранить новые данные в состояние.
          val v2 = v0.copy(
            watchers   = watchers3,
            suppressor = supprOpt.map(_._1),
            switch     = switch2
          )

          // Уведомить другие контроллеры о наступлении геолокации.
          val notifyOthersFx = GlPubSignal( Some(loc), v0.switch.scSwitch ).toEffectPure

          ah.updateMaybeSilentFx( v0.switch.onOff ===* v2.switch.onOff )(v2, notifyOthersFx)

        }
      } else {
        // Данная геолокация подпадает под подавление. Скорее всего что-то пошло не так.
        //LOG.warn(WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = (loc, v0.suppressor))
        noChange
      }


    // Активация/деактивация геолокации.
    case m: GeoLocOnOff =>
      val v0 = value

      // Сборка GlPubSignal, который обычно (но не всегда) нужен.
      def glPubErrFx = GlPubSignal( None, m.scSwitch.orElse(v0.switch.scSwitch) )
        .toEffectPure

      // Оформляем всю логику через option, чтобы не было паутины if:
      Some(m.enabled)
        // Нельзя менять состояние off=>off или on=>on:
        .filterNot {
          v0.switch.onOff contains _
        }
        // Нельзя включать геолокацию, если она жестко выключена юзером:
        .filter { nextEnabled =>
          !nextEnabled || (!v0.switch.hardLock || m.isHard)
        }
        .fold {
          // Нельзя включить геолокацию сейчас.
          if (v0.switch.hardLock && !m.isHard) {
            // Жесткое выключение. Уведомить другие подсистемы, что в итоге нет геолокации:
            val v2 = v0.withSwitch(
              v0.switch.withOutScSwitch
            )
            updatedSilent(v2, glPubErrFx)

          } else {
            // И даже уведомлять никого не надо ни о чём (но надо, если задан scSwitch-контекст).
            m.scSwitch.fold(noChange) { _ =>
              effectOnly(glPubErrFx)
            }
          }
        } {
          // Включение геолокации:
          case true =>
             GeoLocAh._geoLocApiOpt.fold {
              // should never happen(?)
              LOG.warn( ErrorMsgs.GEO_LOC_FAILED, msg = m )
              effectOnly(glPubErrFx)
            } { geoApi =>
              // Запустить мониторинг геолокации.
              val needWatchers =
                if (m.onlyTypes.isEmpty) GeoLocTypes.all
                else m.onlyTypes

              val watchers2 = needWatchers
                .startWatchers(geoApi)
                .toMap

              val onOff2 = v0.switch.onOff.ready( true ).pending()

              var switch2 = v0.switch
                .withOnOff( onOff2 )
              if (m.scSwitch.nonEmpty)
                switch2 = switch2.withScSwitch( m.scSwitch )

              val v2 = v0.copy(
                watchers = v0.watchers ++ watchers2,
                switch   = switch2,
              )
              ah.updateMaybeSilent(v0.switch.onOff ==* v2.switch.onOff)(v2)
            }

          // Заглушить геолокацию и suppressor.
          case false =>
            val v2 = GeoLocAh.doDisable(v0, m.isHard)

            // TODO Нужна поддержка частичной остановки watcher'ов. Пока assert для защиты от ошибочного использования нереализованной частичной остановки.
            assert( m.onlyTypes.isEmpty )

            ah.updateMaybeSilent(v0.switch.onOff ==* v2.switch.onOff)(v2)
        }


    // Сработал таймер окончания подавления suppressor'ом.
    case m: GlSuppressTimeout =>
      val v0 = value

      val resOpt = for {
        s     <- v0.suppressor
        if s.generation ==* m.generation
        glApi <- GeoLocAh._geoLocApiOpt
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
        LOG.warn( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
        noChange
      }


    // Ошибка считывания геолокации.
    case m: GlError =>
      val v0 = value
      // Подхватываем ошибку, если есть куда записывать её.
      // Игнорим _isLocNotSuppressed(), т.к. ошибка не ломает возможное значение внутри Pot.
      v0.watchers
        .get( m.glType )
        .fold {
          // Should never happen.
          LOG.error( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
          noChange
        } { watcher0 =>
          // Если onOff - pending, то сохранить ошибку:
          val notifyOthersFx = GlPubSignal( Some(m), v0.switch.scSwitch ).toEffectPure

          if (m.error.domError.code ==* Html5GeoLocApiErrors.PERMISSION_DENIED) {
            // TODO Отрабатывать так для всех ошибок? Ведь таймаут геолокации и POSITION_UNAVAILABLE аналогичны по сути:
            // Если ошибка DENIED, то выключить геолокацию жестко:
            val v1 = if (v0.switch.scSwitch.isEmpty) v0
                     else v0.withSwitch( v0.switch.withOutScSwitch )

            val v2 = GeoLocAh.doDisable(v1, isHard = true, withException = m.error)
            updated(v2, notifyOthersFx)

          } else {
            // Т.к. геолокация качается из нескольких источников, то остальные ошибки отрабатываются отдельно.
            // TODO Возможно, это неправильно, и геолокация надо сразу вырубать при любой ошибке.
            LOG.warn( ErrorMsgs.GEO_LOC_FAILED, msg = m )
            // Сохранить ошибку в lastLoc
            val wa2 = watcher0.withLastPos(
              watcher0.lastPos.fail( m.error )
            )

            val v2 = v0.copy(
              watchers = v0.watchers + (m.glType -> wa2),
              switch   = v0.switch.withOutScSwitch
            )

            if (v2.switch.onOff.isPending) {
              val isOnOff2 = v2.switch.onOff.getOrElse(true)

              val onOff2 = FailedStale(isOnOff2, m.error)
              val v3 = v2.withSwitch(
                v2.switch
                  .withOnOff( onOff2 )
              )
              updated(v3, notifyOthersFx)
            } else {
              updatedSilent(v2, notifyOthersFx)
            }
          }
        }

  }

}


/** Статическая утиль для контроллера [[GeoLocAh]]. */
object GeoLocAh {

  /** Доступ к HTML5 Geolocation API. */
  private def _geoLocApiOpt = WindowVm().geolocation


  /** Доп.операции для списков "наблюдателей" за геолокацией. */
  implicit class WatchersExtOps(val watchers: IterableOnce[(GeoLocType, MGeoLocWatcher)]) {

    def clearWatchers(): Iterator[(GeoLocType, MGeoLocWatcher)] = {
      for {
        glApi         <- _geoLocApiOpt.iterator
        (wtype, w1)   <- watchers.iterator
        w2 = w1.watchId.fold(w1) { watchId =>
          glApi.clearWatch2(watchId)
          w1.copy(watchId = None)
        }
        // Если новый MglWatch пустой получился, то отбросить его.
        if w2.nonEmpty
      } yield {
        wtype -> w2
      }
    }

  }


  /** Выполнить действия выключения геолокации. */
  def doDisable(v0: MScGeoLoc, isHard: Boolean, withException: Exception = null): MScGeoLoc = {
    for (s <- v0.suppressor)
      DomQuick.clearTimeout(s.timerId)

    v0.copy(
      // Выключить все watcher'ы:
      watchers = v0.watchers
        .iterator
        .clearWatchers()
        .toMap,

      // Убрать suppressor, если активен.
      suppressor = None,

      // pending будет заменён на первом GlLocation/GlError.
      switch = v0.switch.copy(

        onOff = {
          var onOff2 = v0.switch.onOff.ready(false)
          if (withException != null)
            onOff2 = onOff2.fail( withException )
          onOff2
        },
        hardLock = isHard,

        // Найти наиболее точное местоположение в списке известных и сохранить в prevGeoLoc:
        prevGeoLoc = {
          val watchers0 = v0.watchers
            .iterator
            .filter(_._2.lastPos.nonEmpty)
            .toSeq
          OptionUtil.maybeOpt( watchers0.nonEmpty ) {
            watchers0
              .iterator
              // Интересует наиболее точная геолокация.
              // TODO Учитывать опциональную gl.accuracyM вместо glType.precision? Ведь GPS может быть не всегда точнее значения Bss.
              .maxBy(_._1.precision)
              ._2.lastPos.toOption
          }
        }

      )

    )
  }

}
