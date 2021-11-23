package io.suggest.sc.c.dev

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.geo.{GeoLocApi, MGeoLoc}
import io.suggest.sc.m.{GeoLocOnOff, GlLeafletApiLocate, GlLeafletApiLocateTimeout}
import io.suggest.sc.m.dev.{GlLeafletLocateArgs, MGlSourceS, MScGeoLoc}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2
import io.suggest.sjs.dom2.{Coordinates, DomQuick}
import io.suggest.spa.DiodeUtil.Implicits.{ActionHandlerExt, EffectsOps}
import io.suggest.spa.DoNothing
import monocle.Traversal
import scalaz.Need

import scala.scalajs.js
import scala.util.Success

final class LeafletGeoLocAh[M](
                                modelRW              : ModelRW[M, MScGeoLoc],
                                geoLocApis           : () => LazyList[GeoLocApi],
                              )
  extends ActionHandler( modelRW )
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {


    // Вызов функции локации внутри Leaflet, но вместо API произошла переброска в текущий circuit.
    case m: GlLeafletApiLocate =>
      val v0 = value
      m.locateOpts.fold {
        // Выключения подписки на геолокацию.
        if (v0.leafletLoc.isEmpty) {
          noChange
        } else {
          val v2 = (MScGeoLoc.leafletLoc replace None)(v0)
          updatedSilent(v2)
        }

      } { leafletLocateOpts =>
        // Запрашивается включение подписки на геолокацию.
        var fxAcc = List.empty[Effect]

        // Если есть текущая геолокация, то вернуть её сразу же:
        // TODO currentLocation надо проверять по maxAge, но пока сейчас дата-время получения ещё не сохраняется в состоянии.
        var alreadyRepliedLocation = false
        for ( (_, geoLoc) <- v0.currentLocation) {
          fxAcc ::= LeafletGeoLocAh.leafletOnLocationFx( leafletLocateOpts, geoLoc )
          alreadyRepliedLocation = true
        }

        // Если геолокация выключена, то надо её включить.
        if (!(v0.switch.onOff contains[Boolean] true))
          fxAcc ::= GeoLocOnOff( enabled = true, isHard = false ).toEffectPure
        else {
          // Надо запустить single-update, чтобы гарантированно получить текущее местоположение.
          for (glApi <- geoLocApis().firstAvailable) {
            fxAcc ::= Effect {
              glApi
                .getPosition()
                .transform {
                  case _ =>
                    Success(DoNothing)
                }
            }
          }
        }

        // Если задан timeout, то запустить таймер.
        val timeoutNeedOpt = for {
          timeoutMs <- leafletLocateOpts.locateOpts.timeout.toOption
        } yield {
          Need {
            DomQuick.timeoutPromiseT( timeoutMs )( GlLeafletApiLocateTimeout )
          }
        }
        for (timeoutNeed <- timeoutNeedOpt)
          fxAcc ::= Effect( timeoutNeed.value.fut )

        val v2Opt = if (
          !alreadyRepliedLocation ||
            (leafletLocateOpts.locateOpts.watch contains[Boolean] true)
        ) {
          // Нужно ждать получения локации.
          // Закрыть старый таймаут, если есть.
          for (fx <- LeafletGeoLocAh.cancelLeafletTimeout(v0.leafletLoc))
            fxAcc ::= fx

          val v2 = (MScGeoLoc.leafletLoc replace Some(
            MGlSourceS(
              args            = leafletLocateOpts,
              timeoutId       = timeoutNeedOpt
                .map( need => () => need.value.timerId ),
            )
          ))(v0)
          Some( v2 )

        } else if (alreadyRepliedLocation && v0.leafletLoc.nonEmpty) {
          // Уже отправлен одноразовый ответ геолокации. Надо удалить старые leafletLoc-данные из состояния:
          val v2 = (MScGeoLoc.leafletLoc replace None)(v0)
          Some( v2 )

        } else {
          None
        }

        ah.optionalResult( v2Opt, fxAcc.mergeEffects, silent = true )
      }


    case GlLeafletApiLocateTimeout =>
      val v0 = value
      (for {
        glSrcS <- v0.leafletLoc
      } yield {
        v0.currentLocation.fold {
          // Отправить ошибку по таймауту:
          val locErrorFx = Effect.action {
            val posErr = new dom2.PositionError {
              // Используем не-нативные константы PositionError, т.к. внутри WebView нет гарантий, что в window определён тип PositionError.*
              override val code = dom2.PositionError.TIMEOUT
              override val message = "Timeout"
            }
            glSrcS.args.onLocError( posErr )
            DoNothing
          }

          // Если watch, то подчистить данные по таймеру. Иначе - вычистить сразу всё.
          val v2 = (MScGeoLoc.leafletLoc replace None)(v0)
          updatedSilent(v2, locErrorFx)

        } { case (_, currLoc) =>
          // Отправить текущее местоположение, если ещё не отправлено.
          val fx = LeafletGeoLocAh.leafletOnLocationFx( glSrcS.args, currLoc )
          // Если !watch, то почистить состояние целиком. Иначе - снести только timeoutId.
          val v2 = if (glSrcS.args.locateOpts.watch contains[Boolean] true) {
            // Чистка только данных по таймеру.
            MScGeoLoc.leafletLoc
              .andThen( Traversal.fromTraverse[Option, MGlSourceS] )
              .andThen( MGlSourceS.timeoutId )
              .replace( None )(v0)
          } else {
            // Без watch, поэтому просто чистим состояние целиком.
            MScGeoLoc.leafletLoc.replace( None )(v0)
          }

          updatedSilent(v2, fx)
        }
      })
        .getOrElse( noChange )

  }

}


object LeafletGeoLocAh {

  /** Effect of copy-pasting of data Эффект переброса данных в leaflet функцию onLocation. */
  def leafletOnLocationFx(lOpts: GlLeafletLocateArgs, geoLoc: MGeoLoc): Effect = {
    Effect.action {
      val _coords = new Coordinates {
        override val longitude = geoLoc.point.lon.toDouble
        override val latitude = geoLoc.point.lat.toDouble
        override val accuracy = geoLoc.accuracyOptM getOrElse 1000.0
      }
      val pos = new dom2.Position {
        override val timestamp = js.Date.now()    // TODO Брать реальный timestamp, а не выдумывать на ходу.
        override val coords = _coords
      }
      lOpts.onLocation( pos )
      DoNothing
    }
  }



  /** Cancel leaflet location timer effect. */
  def cancelLeafletTimeout( leafletLocOpt: Option[MGlSourceS] ): Option[Effect] = {
    // Закрыть старый таймаут, если есть.
    for {
      leafletLoc0 <- leafletLocOpt
      timer0 <- leafletLoc0.timeoutId
    } yield {
      Effect.action {
        DomQuick.clearTimeout( timer0() )
        DoNothing
      }
    }
  }

}
