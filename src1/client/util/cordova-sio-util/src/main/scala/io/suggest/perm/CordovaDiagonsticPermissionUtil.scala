package io.suggest.perm

import cordova.Cordova
import cordova.plugins.diagnostic.{BluetoothState_t, LocationMode_t, PermissionStatus_t}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.log.Log
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

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
    FutureUtil.tryCatchFut {
      for {
        res <- Cordova.plugins.diagnostic.getLocationAuthorizationStatusF()
      } yield {
        CdpGeoLocPermData( res )
      }
    }
  }


  /** Read bluetooth permission state.
    *
    * @param osFamily Device operating system.
    * @param config Read data from current configuration.
    * @return
    */
  def getBlueToothPermissionState(osFamily: MOsFamily, configFut: => Future[Option[Boolean]]): Future[IPermissionState] = {
    FutureUtil.tryCatchFut {
      osFamily match {
        case MOsFamilies.Android =>
          Cordova.plugins.diagnostic
            .isBluetoothAvailableF()
            .flatMap[IPermissionState] { isAvail =>
              if (!isAvail)
                Future successful BoolOptPermissionState( OptionUtil.SomeBool.someFalse )
              else
                getGeoLocPerm()
            }
        case MOsFamilies.Apple_iOS =>
          configFut
            .recover { case _ => None }
            .flatMap { isGrantedOpt =>
              if (isGrantedOpt.isEmpty) {
                Future successful BoolOptPermissionState( None )
              } else {
                Cordova.plugins.diagnostic.getBluetoothStateF()
                  .map( CdpBlueToothData.apply )
              }
            }
      }
    }
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

  override def value = permStatus

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

  override def value = btState

  override def isPoweredOn: Boolean =
    btState ==* BtStates.POWERED_ON

  override def isGranted: Boolean =
    !isDenied

  override def isDenied: Boolean =
    BtStates.UNAUTHORIZED contains btState

  override def isPrompt: Boolean =
    btState ==* BtStates.UNKNOWN

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
