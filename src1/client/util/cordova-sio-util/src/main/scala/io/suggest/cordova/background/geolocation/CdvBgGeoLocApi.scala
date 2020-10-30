package io.suggest.cordova.background.geolocation

import cordova.plugins.background.geolocation._
import io.suggest.geo._
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.GeoLocWatchId_t
import japgolly.univeq._

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.2020 11:57
  * Description: Реализация GeoLocApi поверх cordova-plugin-background-geolocation.
  */
final class CdvBgGeoLocApi extends GeoLocApi with Log {

  override def underlying =
    Option.when( isAvailable() )( CdvBackgroundGeolocation )


  override def isAvailable() =
    CdvBgGeo.isAvailable()


  override def getAndWatchPosition(options: GeoLocApiWatchOptions): Future[GeoLocWatchId_t] = {
    // Подписаться на события геолокации:
    Try {
      CdvBackgroundGeolocation.removeAllListeners()
    }

    val _isHighAccuracy = options.watcher.highAccuracy contains[Boolean] true

    val _maxAgeMsU = options.watcher
      .maxAge
      .map( _.toMillis.toInt )
      .orUndefined

    val _onLocationF = { loc: Location =>
      val mgl = MGeoLoc(
        point = MGeoPoint(
          lat = loc.latitude,
          lon = loc.longitude
        ),
        accuracyOptM = loc.accuracy.toOption
      )
      options.onLocation( mgl )
      mgl
    }

    val currLocP = Promise[MGeoLoc]()
    val _onErrorOptF = for (onErrorF <- options.onError) yield {
      {bgGeoError: BackgroundGeolocationError =>
        val ex = PositionException(
          bgGeoError.code,
          bgGeoError.message,
          isPermissionDenied = bgGeoError.code ==* CdvBackgroundGeolocation.PERMISSION_DENIED,
          raw = bgGeoError,
        )
        onErrorF( ex )
        ex
      }
    }

    // Немедленный запуск получения текущего местоположения:
    CdvBackgroundGeolocation.getCurrentLocation(
      success = _onLocationF andThen currLocP.success,
      fail = _onErrorOptF
        .map(_ andThen currLocP.failure)
        .getOrElse(null),
      options = new LocationOptions {
        override val maximumAge = _maxAgeMsU
        override val enableHighAccuracy = _isHighAccuracy
      },
    )

    // Запуск фонового мониторинга:
    for {
      _ <- currLocP.future
      // Сконфигурировать фоновый мониторинг:
      configFut = CdvBackgroundGeolocation.configureF(
        new ConfigOptions {
          // TODO Для background-геолокации задействовать DISTANCE_FILTER_PROVIDER
          override val locationProvider = CdvBackgroundGeolocation.ACTIVITY_PROVIDER

          // У приложения пока геолокация не работает в фоне.
          override val startOnBoot = false
          override val saveBatteryOnBackground = true
          override val stopOnTerminate = true

          override val desiredAccuracy = {
            if (_isHighAccuracy) {
              CdvBackgroundGeolocation.HIGH_ACCURACY
            } else {
              CdvBackgroundGeolocation.MEDIUM_ACCURACY
            }
          }
          override val fastestInterval = _maxAgeMsU

        }
      )
      _ = {
        CdvBackgroundGeolocation.onLocation( _onLocationF )
        for (onErrorF <- _onErrorOptF)
          CdvBackgroundGeolocation.onError( onErrorF )
      }
      _ <- configFut
    } yield {
      CdvBackgroundGeolocation.start()
      js.undefined
    }
  }


  override def clearWatch(watchIds: GeoLocWatchId_t): Future[_] = {
    Future {
      Try {
        CdvBackgroundGeolocation.removeAllListeners()
      }
        .failed
        .foreach { ex =>
          logger.error( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, ex, watchIds )
        }

      CdvBackgroundGeolocation.stop()
    }
  }

}
