package io.suggest.cordova.background.geolocation

import cordova.plugins.background.geolocation._
import io.suggest.geo._
import io.suggest.i18n.MsgCodes
import io.suggest.log.Log
import io.suggest.msg.{ErrorMsgs, Messages}
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
final class CdvBgGeoLocApi(
                            getMessages: () => Messages,
                          )
  extends GeoLocApi
  with Log
{

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
    if (options.watcher.watch) {
      for {
        _ <- currLocP.future
        // Сконфигурировать фоновый мониторинг
        messages = getMessages()
        configFut = CdvBackgroundGeolocation.configureF(
          new ConfigOptions {
            // TODO Для background-геолокации задействовать DISTANCE_FILTER_PROVIDER
            override val locationProvider = CdvBackgroundGeolocation.ACTIVITY_PROVIDER

            // У приложения пока геолокация не работает в фоне.
            override val startOnBoot = false
            override val stopOnTerminate = true
            // TODO Не ясно, что с сервисом. Надо будет разобраться. Наверное, он нужен.
            //override val startForeground = false
            override val desiredAccuracy = {
              if (_isHighAccuracy) {
                CdvBackgroundGeolocation.HIGH_ACCURACY
              } else {
                CdvBackgroundGeolocation.MEDIUM_ACCURACY
              }
            }
            override val fastestInterval = _maxAgeMsU

            // iOS
            // saveBatteryOnBackground: это для некоего background, поэтому не ясно, актуально ли для foreground-работы.
            override val saveBatteryOnBackground = true

            // Android:
            override val startForeground = false
            override val notificationsEnabled = false
            override val notificationTitle = messages( MsgCodes.`Bg.location` )
            override val notificationText = messages( MsgCodes.`Bg.location.hint` )
            //override val notificationIconSmall = "res://ic_notification"
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

    } else {
      Future.successful( js.undefined )
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
