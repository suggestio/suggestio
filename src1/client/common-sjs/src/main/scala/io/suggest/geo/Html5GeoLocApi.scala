package io.suggest.geo

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.sjs.dom2._

import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.2020 23:45
  * Description:
  */

/** Реализация [[GeoLocApi]] поверх стандартного HTML5 Geolocation API.
  * Подходит для cordova-plugin-geolocation. */
final class Html5GeoLocApi extends GeoLocApi {

  override def underlying =
    WindowVm().geolocation

  override def isAvailable(): Boolean =
    underlying.isDefined

  override def watchPosition( options: GeoLocApiWatchOptions ): Future[GeoLocWatchId_t] = {
    Future {
      (for {
        h5GeoLocApi <- underlying

        onSuccessF = { pos: dom.Position =>
          val geoLoc = MGeoLoc(
            point        = MGeoPointJs( pos.coords ),
            accuracyOptM = Some( pos.coords.accuracy ),
          )
          options.onLocation( geoLoc )
        }: js.Function1[dom.Position, _]

        onPosErrorOptF = options
          .onError
          .map { onErrorF =>
            {posError: dom.PositionError =>
              val posEx = PositionException(
                posError.code,
                posError.message,
                isPermissionDenied = posError.code ==* Html5GeoLocApiErrors.PERMISSION_DENIED,
                raw = posError,
              )
              onErrorF( posEx )
            }: js.Function1[dom.PositionError, _]
          }

        posOptions = new PositionOptions {
          override val enableHighAccuracy = options.watcher.highAccuracy.orUndefined
          override val maximumAge         = options.watcher.maxAge
            .map(_.toMillis.toDouble)
            .orUndefined
        }

      } yield {
        // Запросить текущее местоположение:
        h5GeoLocApi.getCurrentPosition(
          successCallback = onSuccessF,
          errorCallback   = onPosErrorOptF.orNull,
          options         = posOptions,
        )

        // Подписка на изменение геолокации.
        h5GeoLocApi.watchPosition2(
          successCallback = onSuccessF,
          errorCallback   = onPosErrorOptF.orNull,
          options         = posOptions,
        )
      })
        .orUndefined
    }
  }

  override def clearWatch(watchId: GeoLocWatchId_t): Future[_] = {
    Future {
      for (api <- underlying)
        api.clearWatch2( watchId )
    }
  }

}
