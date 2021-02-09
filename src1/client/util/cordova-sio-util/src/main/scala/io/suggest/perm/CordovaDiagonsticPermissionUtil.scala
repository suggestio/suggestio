package io.suggest.perm

import cordova.Cordova
import cordova.plugins.diagnostic.{BluetoothState_t, LocationMode_t, PermissionStatus_t}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.{Future, Promise}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.19 12:21
  * Description: Поддержка унифицированного доступа к пермишшенам
  */
object CordovaDiagonsticPermissionUtil extends Log {

  /** Получение данных пермишшена. */
  def getGeoLocPerm(): Future[CdpGeoLocPermData] = {
    // Вызов нативной функции с подпиской на события:
    try {
      val geoLocP = Promise[PermissionStatus_t]()
      Cordova.plugins.diagnostic.getLocationAuthorizationStatus(
        geoLocP.success,
        message =>
          geoLocP.failure( new RuntimeException(message) )
      )
      for (res <- geoLocP.future) yield {
        CdpGeoLocPermData( res )
      }
    } catch {
      case ex: Throwable =>
        logger.warn( ErrorMsgs.PERMISSION_API_FAILED, ex, ErrorMsgs.GEO_LOCATION_FAILED )
        Future.failed( ex )
    }
  }


  /** Состояние доступности bluetooth. */
  def getBlueToothState(): Future[CdpBlueToothData] = {
    val btStateP = Promise[BluetoothState_t]()
    try {
      Cordova.plugins.diagnostic.getBluetoothState(
        btStateP.success,
        message =>
          btStateP.failure( new RuntimeException(message) )
      )
    } catch {
      case ex: Throwable =>
        btStateP.tryFailure( ex )
    }

    for (btState <- btStateP.future) yield
      CdpBlueToothData( btState )
  }


  def PermStatuses =
    Cordova.plugins.diagnostic.permissionStatus

  def BtStates =
    Cordova.plugins.diagnostic.bluetoothState

  def AndroidLocationModes =
    Cordova.plugins.diagnostic.locationModeU

}


import CordovaDiagonsticPermissionUtil._


/** Обёртка для прав Реализация [[IPermissionState]] поверх статуса абстрактного пермишшена.
  *
  * @param permStatus Итоговый статус пермишшена.
  */
case class CdpGeoLocPermData( permStatus: PermissionStatus_t ) extends IPermissionState {

  override def isPoweredOn: Boolean =
    true

  override def isGranted: Boolean =
    permStatus ==* PermStatuses.GRANTED

  override def isDenied: Boolean = {
    (permStatus ==* PermStatuses.DENIED) ||
    (PermStatuses.DENIED_ALWAYS contains permStatus) ||
    (PermStatuses.RESTRICTED contains permStatus)
  }

  override def isPrompt: Boolean =
    permStatus ==* PermStatuses.NOT_REQUESTED

  override def hasOnChangeApi: Boolean = true

  override def onChange(f: IPermissionState => _): Unit = {
    Cordova.plugins.diagnostic.registerLocationStateChangeHandler { locOrPermMode =>
      val pss = this.snapshot(
        isGranted =
          (locOrPermMode.asInstanceOf[PermissionStatus_t] ==* PermStatuses.GRANTED) ||
          AndroidLocationModes.exists { ls =>
            !(locOrPermMode.asInstanceOf[LocationMode_t] ==* ls.LOCATION_OFF)
          }
      )
      f(pss)
    }
  }

  override def onChangeReset(): Unit =
    Cordova.plugins.diagnostic.registerLocationStateChangeHandler()

}


/** Доступ к мониторингу прав доступа на Bluetooth.
  *
  * @param btState Новое состояние Bluetooth.
  */
case class CdpBlueToothData( btState: BluetoothState_t ) extends IPermissionState {

  override def isPoweredOn: Boolean =
    btState ==* BtStates.POWERED_ON

  override def isGranted: Boolean =
    !isDenied

  override def isDenied: Boolean =
    BtStates.UNAUTHORIZED contains btState

  override def isPrompt: Boolean =
    btState ==* BtStates.UNKNOWN    // TODO Вероятно, тут должно быть всегда true?

  override def hasOnChangeApi: Boolean = true

  override def onChange(f: IPermissionState => _): Unit = {
    Cordova.plugins.diagnostic.registerBluetoothStateChangeHandler { btState =>
      val pss = this.snapshot(
        isGranted =
          (btState ==* BtStates.POWERED_ON) ||
          (BtStates.POWERING_ON contains btState)
      )
      f(pss)
    }
  }

  override def onChangeReset(): Unit =
    Cordova.plugins.diagnostic.registerBluetoothStateChangeHandler()

}
