package io.suggest.ble.api

import io.suggest.ble.BeaconDetected
import io.suggest.ble.api.cordova.ble.CordovaBleApi
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MOsFamily
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import japgolly.univeq._

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:21
  * Description: Интерфейс для конкретных API'шек работы с маячками.
  * Изначально была реализация для cordova-ble, но в планах была ещё реализовать web-bluetooth.
  */

trait IBleBeaconsApi {

  /** Узнать, включён ли bluetooth в данный момент? */
  def isBleEnabled(): Future[Boolean]

  /** Попробовать включить bluetooth. */
  def enableBle(): Future[Boolean]

  /** Доступно ли текущее API для использования? */
  def isApiAvailable: Boolean

  /** Начать слушанье ble-маячков, отсылая данные в указанный fsm. */
  def listenBeacons(opts: IBleBeaconsApi.ListenOptions): Future[_]

  /** Прекратить любое слушанье маячков. */
  def unListenAllBeacons(): Future[_]

  /** Требуется ли перезапуск BLE-сканнера?
    * Тут НЕ сравниваются инстансы функций onBeacon и проч. Интересует только сверка настроек сканирования. */
  def isScannerRestartNeededSettingsOnly(
                                          v0: IBleBeaconsApi.ListenOptions,
                                          v2: IBleBeaconsApi.ListenOptions,
                                          osFamily: Option[MOsFamily],
                                        ): Boolean

  /** Требуется ли перезапуск BLE-сканнера? Сравнение всех элементов ListenOptions. */
  def isScannerRestartNeeded(v0: IBleBeaconsApi.ListenOptions,
                             v2: IBleBeaconsApi.ListenOptions,
                             osFamily: Option[MOsFamily]): Boolean = {
    (v0.onBeacon eq v2.onBeacon) &&
    isScannerRestartNeededSettingsOnly(v0, v2, osFamily)
  }

  override def toString = getClass.getSimpleName

}


object IBleBeaconsApi extends Log {

  case class ListenOptions(
                            onBeacon          : BeaconDetected => Unit,
                            scanMode          : ScanMode,
                          )
  object ListenOptions {
    @inline implicit def univEq: UnivEq[ListenOptions] = UnivEq.force
  }


  type ScanMode = Int
  object ScanMode {
    /** Подразумевается скан без скана: если другие приложения/сервисы будут слушать эфир, то сюда бонусом упадут возможные результаты, если повезёт. */
    final def OPPORTUNISTIC = 0.asInstanceOf[ScanMode]
    /** Скан с длинными паузами. */
    final def LOW_POWER = 1.asInstanceOf[ScanMode]
    /** Скан и паузы в сканировании примерно поровну. */
    final def BALANCED = 2.asInstanceOf[ScanMode]
    /** Скан без пауз. */
    final def FULL_POWER = 3.asInstanceOf[ScanMode]
  }


  /** Найти доступные API для маячков. */
  def detectApis(): Seq[IBleBeaconsApi] = {
    (for {
      cordovaBleApi <- Try( new CordovaBleApi )
      apiAvail <- {
        val availTry = Try( cordovaBleApi.isApiAvailable )
        for (ex <- availTry.failed)
          logger.log( ErrorMsgs.BLE_BEACONS_API_AVAILABILITY_FAILED + " " + cordovaBleApi, ex )
        availTry
      }
    } yield {
      OptionUtil
        .maybe(apiAvail)( cordovaBleApi )
        .toList
    })
      .getOrElse( Nil )
  }

  @inline implicit def univEq: UnivEq[IBleBeaconsApi] = UnivEq.force

}


