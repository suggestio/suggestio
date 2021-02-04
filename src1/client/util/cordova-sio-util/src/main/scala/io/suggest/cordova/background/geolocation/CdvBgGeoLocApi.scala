package io.suggest.cordova.background.geolocation

import cordova.plugins.background.geolocation._
import io.suggest.geo._
import io.suggest.i18n.MsgCodes
import io.suggest.log.Log
import io.suggest.msg.{ErrorMsgs, Messages}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2
import japgolly.univeq._

import scala.concurrent.Future
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

  private var _options: Option[GeoLocApiWatchOptions] = None

  private var _isHighAccuracy: Boolean = false
  private var _maxAgeMsU: js.UndefOr[Int] = js.undefined
  private var _onLocationF: js.Function1[Location, Unit] = null
  private var _onErrorOptF: Option[js.Function1[BackgroundGeolocationError, Unit]] = None


  override def underlying =
    Option.when( isAvailable() )( CdvBackgroundGeolocation )


  override def isAvailable() =
    CdvBgGeo.isAvailable()


  override def configure(options: GeoLocApiWatchOptions): Future[_] = {
    _options = Some(options)

    _isHighAccuracy = options.watcher.highAccuracy contains[Boolean] true

    _maxAgeMsU = options.watcher
      .maxAge
      .map( _.toMillis.toInt )
      .orUndefined

    _onLocationF = { loc: Location =>
      val mgl = MGeoLoc(
        point = MGeoPoint(
          lat = loc.latitude,
          lon = loc.longitude
        ),
        accuracyOptM = loc.accuracy.toOption
      )
      options.onLocation( mgl )
    }

    _onErrorOptF = for {
      onErrorF <- options.onError
    } yield {
      {bgGeoError: BackgroundGeolocationError =>
        val ex = PositionException(
          bgGeoError.code,
          bgGeoError.message,
          isPermissionDenied = bgGeoError.code ==* CdvBackgroundGeolocation.PERMISSION_DENIED,
          raw = bgGeoError,
        )
        onErrorF( ex )
      }
    }

    val messages = getMessages()
    for {

      // Подписаться на события геолокации:
      _ <- CdvBackgroundGeolocation.configureF(
        new ConfigOptions {
          // TODO Для background-геолокации задействовать DISTANCE_FILTER_PROVIDER, для андройда попробовать ACTIVITY_PROVIDER
          override val locationProvider = CdvBackgroundGeolocation.DISTANCE_FILTER_PROVIDER

          // У приложения пока геолокация не работает в фоне.
          override val startOnBoot = false
          override val stopOnTerminate = true
          // TODO Не ясно, что с сервисом. Надо будет разобраться. Наверное, он нужен.
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
          override val startForeground = true
          override val notificationsEnabled = false
          override val notificationTitle = messages( MsgCodes.`Bg.location` )
          override val notificationText = messages( MsgCodes.`Bg.location.hint` )
          //override val notificationIconSmall = "res://ic_notification"
        }
      )

    } yield {
      // Подписаться на события геолокации:
      CdvBackgroundGeolocation.onLocation( _onLocationF )

      for (onErrorF <- _onErrorOptF) {
        CdvBackgroundGeolocation.onError { bgGeoError =>
          // Отфильтровываем таймауты за ненадобностью, т.к. внизу запускается getCurrentLocation(),
          if (bgGeoError.code !=* dom2.PositionError.TIMEOUT)
            onErrorF(bgGeoError)
          else
            logger.log( ErrorMsgs.IGNORED_EXCEPTION, msg = (bgGeoError.code, bgGeoError.message) )
        }
      }
    }
  }


  override def getAndWatchPosition(): Future[_] = {
    // Запустить фоновый мониторин геолокации:
    Future {
      CdvBackgroundGeolocation.start()
    }
      // Сразу запустить текущую геолокацию:
      .andThen( _ => getPosition() )
  }


  override def getPosition(): Future[_] = {
    // getCurrentLocation() - метод, блокирующий метод плагина, умеет наглухо вешать приложение при запуске, если забыть указать timeout.
    // Используем его для запуска single update, чтобы получить текущие координаты локацию как можно скорее (придут в onLocation).
    CdvBackgroundGeolocation.getCurrentLocationF(
      options = new LocationOptions {
        // Выставить маленький таймаут, чтобы просто запустить singleUpdate-геолокацию, и вернуть управление.
        override val timeout = 75
        override val maximumAge = _maxAgeMsU
        override val enableHighAccuracy = _isHighAccuracy
      },
    )
  }


  override def clearWatch() = Future {
    CdvBackgroundGeolocation.stop()
  }


  override def reset(): Future[_] = {
    clearWatch()

    Try {
      CdvBackgroundGeolocation.removeAllListeners()
    }
    _onLocationF = null
    _onErrorOptF = None
    _options = None
    _isHighAccuracy = false
    _maxAgeMsU = js.undefined

    Future.successful()
  }

}
