package io.suggest.leaflet

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.geo.{GeoLocApi, MGeoLoc}
import io.suggest.sc.model.dev.MScGeoLoc
import io.suggest.sc.model.GeoLocOnOff
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2
import io.suggest.sjs.dom2.{Coordinates, DomQuick}
import io.suggest.spa.DiodeUtil.Implicits.{ActionHandlerExt, EffectsOps}
import io.suggest.spa.DoNothing
import monocle.Traversal
import org.scalajs.dom
import scalaz.Need

import scala.scalajs.js
import scala.util.Success

/** This controller handles some actions for patched leaflet.js.
  * Monkey patching via Sc3LeafletOverrides is used, when HTML5 Geolocation API is unavalable (like cordova-background-geolocation).
  * After api patches, some leaflet location calls translated into actions for current controller.
  */
final class LeafletGeoLocAh[M](
                                modelRW              : ModelRW[M, MScGeoLoc],
                                geoLocApis           : () => LazyList[GeoLocApi],
                              )
  extends ActionHandler( modelRW )
  with ILeafletGeoLocAh[M]
{ ah =>

  import scala.language.implicitConversions
  implicit private def i2mGlSource(i: IGlSourceS): MGlSourceS =
    i.asInstanceOf[MGlSourceS]

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
          fxAcc ::= leafletOnLocationFx( leafletLocateOpts, geoLoc )
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
          for (fx <- cancelLeafletTimeout(v0.leafletLoc))
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
          val fx = leafletOnLocationFx( glSrcS.args, currLoc )
          // Если !watch, то почистить состояние целиком. Иначе - снести только timeoutId.
          val v2: MScGeoLoc = if (glSrcS.args.locateOpts.watch contains[Boolean] true) {
            // Чистка только данных по таймеру.
            MScGeoLoc.leafletLoc
              .andThen( Traversal.fromTraverse[Option, IGlSourceS] )
              .replace {
                MGlSourceS.timeoutId.replace( None )( glSrcS )
              }(v0)
          } else {
            // Без watch, поэтому просто чистим состояние целиком.
            MScGeoLoc.leafletLoc.replace( None )(v0)
          }

          updatedSilent(v2, fx)
        }
      })
        .getOrElse( noChange )

  }


  /** Effect for transferring location data into Leaflet's onLocation callback. */
  override def leafletOnLocationFx(geoLoc: MGeoLoc): Effect =
    leafletOnLocationFx( value.leafletLoc.get.args, geoLoc )

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


  def cancelLeafletTimeout( leafletLocOpt: Option[IGlSourceS] ): Option[Effect] = {
    // Закрыть старый таймаут, если есть.
    for {
      leafletLoc0 <- leafletLocOpt
      timer0 <- leafletLoc0.timeoutId
    } yield {
      Effect.action {
        js.timers.clearTimeout( timer0() )
        DoNothing
      }
    }
  }

  override def isWatching(): Boolean = {
    value
      .leafletLoc
      .exists( _.args.locateOpts.watch contains[Boolean] true )
  }

  override def onLocationError(): Option[dom.PositionError => Unit] = {
    value
      .leafletLoc
      .map( _.args.onLocError )
  }

}
