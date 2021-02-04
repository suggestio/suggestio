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
  * Description: Обёртка поверх HTML5 API.
  */

/** Реализация [[GeoLocApi]] поверх стандартного HTML5 Geolocation API.
  * Подходит для cordova-plugin-geolocation. */
final class Html5GeoLocApi extends GeoLocApi {

  private var _options: Option[GeoLocApiWatchOptions] = None
  private var _watchId: Option[GeoLocWatchId_t] = None

  private var _onSuccessF: js.Function1[dom.Position, Unit] = null
  private var _onPosErrorOptF: Option[js.Function1[dom.PositionError, Unit]] = None
  private var _domPositionOptions: PositionOptions = null


  override def configure(options: GeoLocApiWatchOptions): Future[_] = {
    _options = Some( options )

    _onSuccessF = { pos: dom.Position =>
      val geoLoc = MGeoLoc(
        point        = MGeoPointJs( pos.coords ),
        accuracyOptM = Some( pos.coords.accuracy ),
      )
      options.onLocation( geoLoc )
    }: js.Function1[dom.Position, Unit]

    _onPosErrorOptF = options
      .onError
      .map { onErrorF =>
        {posError: dom.PositionError =>
          val posEx = PositionException(
            posError.code,
            posError.message,
            isPermissionDenied = (posError.code ==* Html5GeoLocApiErrors.PERMISSION_DENIED),
            raw = posError,
          )
          onErrorF( posEx )
        }: js.Function1[dom.PositionError, Unit]
      }

    _domPositionOptions = new PositionOptions {
      override val enableHighAccuracy = options.watcher.highAccuracy.orUndefined
      override val maximumAge         = options.watcher.maxAge
        .map(_.toMillis.toDouble)
        .orUndefined
    }

    Future.successful()
  }


  override def underlying =
    WindowVm().geolocation

  override def isAvailable(): Boolean =
    underlying.isDefined

  override def getAndWatchPosition(): Future[_] = {
    Future {
      (for {
        h5GeoLocApi <- underlying
      } yield {
        // Подписка на изменение геолокации.
        _watchId = Some {
          h5GeoLocApi.watchPosition2(
            successCallback = _onSuccessF,
            errorCallback   = _onPosErrorOptF.orNull,
            options         = _domPositionOptions,
          )
        }
      })
        .orUndefined
    }
  }


  override def getPosition(): Future[_] = {
    Future {
      for {
        h5GeoLocApi <- underlying
      } {
        h5GeoLocApi.getCurrentPosition(
          successCallback = _onSuccessF,
          errorCallback   = _onPosErrorOptF.orNull,
          options         = _domPositionOptions,
        )
      }
    }
  }


  override def clearWatch(): Future[_] = {
    Future {
      for (watchId <- _watchId; api <- underlying) {
        api.clearWatch2( watchId )
        _watchId = None
      }
    }
  }

  override def reset(): Future[_] = {
    for (_ <- clearWatch()) yield {
      _onSuccessF = null
      _onPosErrorOptF = None
      _domPositionOptions = null
      _options = None
    }
  }

}
