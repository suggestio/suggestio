package io.suggest.sc.c.dev

import diode.data.{FailedStale, Pot, Ready}
import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.geolocation.CdvBgGeoLocApi
import io.suggest.geo._
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.dev.{MGeoLocSwitchS, MGeoLocWatcher, MScGeoLoc, Suppressor}
import io.suggest.sc.m._
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.spa.DiodeUtil.Implicits._
import scala.concurrent.duration._

import scala.util.Success

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

  private def pendingWatch(wTypes: IterableOnce[GeoLocType],
                           watchers0: Map[GeoLocType, MGeoLocWatcher]): Map[GeoLocType, MGeoLocWatcher] = {
    (for {
      wTypeNeed <- wTypes.iterator
    } yield {
      val glWatch = watchers0
        .get( wTypeNeed )
        .filterNot( _.watchId.isPending )
        .map( MGeoLocWatcher.watchId.modify(_.pending()) )
        .getOrElse( MGeoLocWatcher(watchId = Pot.empty.pending()) )

      wTypeNeed -> glWatch
    })
      .toMap
  }

  private def startWatchFx(wTypes: Iterable[GeoLocType], glApi: GeoLocApi) = {
    val maxAgeSome = Some( 20.seconds )
    Option.when(wTypes.nonEmpty)( Effect {
      val glType: GeoLocType = GeoLocTypes.Gps
      glApi
        // Подписка на события геолокации:
        .getAndWatchPosition(
          GeoLocApiWatchOptions(
            onLocation = { geoLoc =>
              dispatcher( GlLocation( glType, geoLoc ) )
            },
            onError = Some { posEx =>
              dispatcher( GlError( glType, posEx ) )
            },
            watcher = GeoLocWatcherInfo(
              highAccuracy  = Some( glType.isHighAccuracy ),
              maxAge        = maxAgeSome,
            ),
          )
        )
        // Завернуть в ответный экшен:
        .transform { tryChanges =>
          val thePot = Pot.empty[GeoLocWatchId_t] withTry tryChanges
          val action = GlModWatchers(
            watchers = Map.empty + (glType -> thePot),
          )
          Success( action )
        }
    })
  }


  private def _isLocNotSuppressed(glType: GeoLocType, v0: MScGeoLoc): Boolean = {
    v0.suppressor.fold(true) { s =>
      glType.precision >= s.minWatch.precision
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Есть на руках местоположение.
    case loc: GlLocation =>
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
            logger.warn( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = loc.glType.toString )
            MGeoLocWatcher( watchId = Pot.empty, lastPos = Ready(loc.location) )
          } {
            MGeoLocWatcher.lastPos.modify(_.ready(loc.location))
          }

          // Откладываем перестройку карты на последний момент: если нужен suppressor, то эта перестройка будет лишней.
          val watchers0 = v0.watchers

          var fxAcc = List.empty[Effect]

          // Если приходят высокоточные данные GPS, то надо отказаться от неточных данных геолокации хотя бы на какое-то время.
          val supprOpt = for {
            ttlMs <- loc.glType.suppressorTtlMs
          } yield {
            // Отменить старый таймер подавления
            for (s <- v0.suppressor) {
              DomQuick.clearTimeout(s.timerId)
            }

            // Найти и отрубить гео-watcher'ы, которые теперь подавляются.
            val (ws4clean, wsKeep) = watchers0.partition {
              case (wtype, _) =>
                wtype.precision <= loc.glType.precision
            }
            val (fxOpt, watchers2) = GeoLocAh.clearWatchers( ws4clean )
            fxOpt.foreach( fxAcc ::= _ )
            val watchers2Iter =  wsKeep ++ watchers2

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

          // Сохранить новые данные в состояние.
          val v2 = v0.copy(
            // Полученный набор гео-вотчеров дополняется ещё и обновлёнными данными текущей геолокации:
            watchers = {
              val tl = loc.glType -> mglw1
              supprOpt.fold(watchers0)(_._2) + tl
            }
              .toMap,
            suppressor = supprOpt.map(_._1),
            switch = {
              // Если pending, то переставить в ready.
              if (v0.switch.onOff.isPending) {
                val isOnOff = v0.switch.onOff.getOrElseTrue
                v0.switch.copy(
                  onOff    = v0.switch.onOff.ready( isOnOff ),
                  scSwitch = None
                )
              } else {
                v0.switch.withOutScSwitch
              }
            },
          )

          // Уведомить другие контроллеры о наступлении геолокации.
          fxAcc ::= GlPubSignal( Some(loc), v0.switch.scSwitch ).toEffectPure

          ah.updateMaybeSilentFx( v0.switch.onOff ===* v2.switch.onOff )(v2, fxAcc.mergeEffects.get)

        }
      } else {
        // Данная геолокация подпадает под подавление. Скорее всего что-то пошло не так.
        //LOG.warn(WarnMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = (loc, v0.suppressor))
        noChange
      }


    // Внутренний экшен обновления watcherIds в состоянии.
    case m: GlModWatchers =>
      if (m.watchers.isEmpty) {
        logger.warn( ErrorMsgs.GEO_WATCH_TYPE_UNSUPPORTED, msg = m )
        noChange

      } else {
        val v0 = value
        val v2 = MScGeoLoc.watchers.modify {
          m.watchers.foldLeft(_) {
            case (acc0, (glType, watcherPot2)) =>
              val glWatcher2 = acc0
                .get( glType )
                .fold {
                  MGeoLocWatcher( watchId = watcherPot2 )
                } (MGeoLocWatcher.watchId set watcherPot2)

              acc0 + (glType -> glWatcher2)
          }
        }(v0)

        updatedSilent(v2)
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
            val v2 = MScGeoLoc.switch
              .modify(_.withOutScSwitch)(v0)
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
            GeoLocAh.GEO_LOC_API.fold {
              // should never happen(?)
              logger.warn( ErrorMsgs.GEO_LOCATION_FAILED, msg = m )
              effectOnly(glPubErrFx)

            } { geoApi =>
              // Запустить мониторинг геолокации.
              val needWatchers0 =
                if (m.onlyTypes.isEmpty) GeoLocTypes.all
                else m.onlyTypes

              val watchers2 = pendingWatch( needWatchers0, v0.watchers )
              val needWatchers2 = watchers2.keys
              val startFxOpt = startWatchFx( needWatchers2, geoApi )

              var switchModAccF = MGeoLocSwitchS.onOff
                .modify( _.ready(true).pending() )
              if (m.scSwitch.nonEmpty)
                switchModAccF = switchModAccF andThen (MGeoLocSwitchS.scSwitch set m.scSwitch)

              val v2 = (
                MScGeoLoc.watchers.modify(_ ++ watchers2) andThen
                MScGeoLoc.switch.modify( switchModAccF )
              )(v0)

              ah.optionalResult( Some(v2), startFxOpt, silent = (v0.switch.onOff ==* v2.switch.onOff) )
            }

          // Заглушить геолокацию и suppressor.
          case false =>
            val (fxOpt, v2) = GeoLocAh.doDisable(v0, m.isHard)

            // TODO Нужна поддержка частичной остановки watcher'ов. Пока assert для защиты от ошибочного использования нереализованной частичной остановки.
            assert( m.onlyTypes.isEmpty )

            ah.optionalResult( Some(v2), fxOpt, silent = (v0.switch.onOff ==* v2.switch.onOff) )
        }


    // Сработал таймер окончания подавления suppressor'ом.
    case m: GlSuppressTimeout =>
      val v0 = value

      (for {
        s <- v0.suppressor
        if s.generation ==* m.generation
        glApi <- GeoLocAh.GEO_LOC_API
      } yield {
        val prevGlTypes0 = s.minWatch.allPrevious
        val watchers2 = pendingWatch( prevGlTypes0, v0.watchers )
        val prevGlTypes2 = watchers2.keys
        val startWatchFxOpt = startWatchFx( prevGlTypes2, glApi )

        val v2 = v0.copy(
          watchers    = v0.watchers ++ watchers2,
          suppressor  = None,
        )
        ah.updatedSilentMaybeEffect( v2, startWatchFxOpt )
      })
        .getOrElse {
          logger.warn( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
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
          logger.error( ErrorMsgs.GEO_UNEXPECTED_WATCHER_TYPE, msg = m )
          noChange
        } { watcher0 =>
          // Если onOff - pending, то сохранить ошибку:
          val notifyOthersFx = GlPubSignal( Some(m), v0.switch.scSwitch ).toEffectPure

          if (m.error.isPermissionDenied) {
            // TODO Отрабатывать так для всех ошибок? Ведь таймаут геолокации и POSITION_UNAVAILABLE аналогичны по сути:
            // Если ошибка DENIED, то выключить геолокацию жестко:
            val v1 =
              if (v0.switch.scSwitch.isEmpty) v0
              else MScGeoLoc.switch.modify(_.withOutScSwitch)(v0)

            val (clearWatchersFxOpt, v2) = GeoLocAh.doDisable(v1, isHard = true, withException = m.error)
            val fx = (notifyOthersFx :: clearWatchersFxOpt.toList)
              .mergeEffects
              .get
            updated( v2, fx )

          } else {
            // Т.к. геолокация качается из нескольких источников, то остальные ошибки отрабатываются отдельно.
            // TODO Возможно, это неправильно, и геолокация надо сразу вырубать при любой ошибке.
            //logger.warn( ErrorMsgs.GEO_LOCATION_FAILED, msg = m )
            // Сохранить ошибку в lastLoc
            val wa2 = MGeoLocWatcher.lastPos
              .modify(_.fail(m.error))(watcher0)

            val v2 = (
              MScGeoLoc.watchers
                .modify( _ + (m.glType -> wa2) ) andThen
              MScGeoLoc.switch
                .modify(_.withOutScSwitch)
            )(v0)

            if (v2.switch.onOff.isPending) {
              val isOnOff2 = v2.switch.onOff getOrElse true

              val onOff2 = FailedStale(isOnOff2, m.error)
              val v3 = MScGeoLoc.switch
                .composeLens(MGeoLocSwitchS.onOff)
                .set(onOff2)(v2)
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

  /** Интерфейс API для геолокации. */
  private lazy val GEO_LOC_API: Option[GeoLocApi] = {
    var apis: LazyList[GeoLocApi] =
      new Html5GeoLocApi #::
      LazyList.empty

    // TODO Что сделать с deviceReady? API плагина доступно только после deviceReady.
    if (CordovaConstants.isCordovaPlatform())
      apis #::= new CdvBgGeoLocApi

    apis.find( _.isAvailable() )
  }

  /** Зачистка watcher'ов. */
  private def clearWatchers(watchers: Map[GeoLocType, MGeoLocWatcher]): (Option[Effect], Map[GeoLocType, MGeoLocWatcher]) = {
    val (keep, forStop) = watchers.partitionMap {
      case a @ (_, glWatcher) =>
        (for (
          watchId <- glWatcher.watchId
          if !glWatcher.watchId.isPending
        ) yield
          a -> watchId
        )
          .toRight( a )
    }

    val fxOpt = OptionUtil.maybeOpt( forStop.nonEmpty ) {
      for (glApi <- GEO_LOC_API) yield {
        Effect.action {
          // Остановка:
          for (cc <- forStop)
            glApi.clearWatch( cc._2 )

          // Вернуть очищенную карту результатов работы:
          val clearedPot = Pot.empty[GeoLocWatchId_t]
          GlModWatchers(
            watchers = (for {
              ((glType, _), _) <- forStop.iterator
            } yield {
              glType -> clearedPot
            })
              .toMap,
          )
        }
      }
    }

    // Зачистка пустых ячеек
    val keep2 = keep
      .iterator
      .filter(_._2.nonEmpty)
      .toList

    val watchers2 = forStop
      .foldLeft( keep2 ) {
        case (acc0, ((glType, glWatch0), _)) =>
          val glWatch2 = MGeoLocWatcher.watchId.modify( _.pending() )( glWatch0 )
          (glType -> glWatch2) :: acc0
      }
      .toMap

    (fxOpt, watchers2)
  }


  /** Выполнить действия выключения геолокации. */
  private def doDisable(v0: MScGeoLoc, isHard: Boolean, withException: Exception = null) = {
    for (s <- v0.suppressor)
      DomQuick.clearTimeout(s.timerId)

    val (clearFxOpt, watchersClean) = clearWatchers( v0.watchers )
    val v2 = v0.copy(
      // Выключить все watcher'ы:
      watchers = watchersClean,

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

    (clearFxOpt, v2)
  }

}
