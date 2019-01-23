package io.suggest.perm

import io.suggest.common.event.DomEvents
import org.scalajs.dom.experimental.permissions._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:13
  * Description: Доступ к Permissions API.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/Permissions_API]]
  */
object Html5PermissionApi {

  /** Доступ к данным разрешения.
    *
    * @param permName id пермишшена.
    * @return Фьючерс с результатом.
    */
  def getGeoLocState(permName: PermissionName ): Future[Html5PermissionData] = {
    try {
      dom.window.navigator
        .permissions
        .query {
          new PermissionDescriptor {
            override val name = permName
          }
        }
        .toFuture
        .map( Html5PermissionData.apply )
    } catch {
      case ex: Throwable =>
        Future.failed( new UnsupportedOperationException(ex) )
    }
  }


  /** Дополнительное API для инстансов HTML5 PermissionState. */
  implicit class PermissionStatusExt( val pState: PermissionState ) extends AnyVal {

    def isGranted: Boolean =
      pState ==* PermissionState.granted

    def isDenied: Boolean =
      pState ==* PermissionState.denied

  }

}


/** Унифицированная обёртка над [[IPermissionState]]. */
case class Html5PermissionData(
                                pStatus: PermissionStatus
                              )
  extends IPermissionState
{

  import Html5PermissionApi._

  /** Управление питанием GPS не затрагивается в html5-спеках. */
  override def isPoweredOn = true

  override def isGranted: Boolean =
    pStatus.state.isGranted

  override def isDenied: Boolean =
    pStatus.state.isDenied

  override def hasOnChangeApi = true

  override def onChange(f: Boolean => _): Unit = {
    pStatus.addEventListenerThis( DomEvents.CHANGE ) {
      (thisStatus: PermissionStatus, _: Event) =>
        f( thisStatus.state.isGranted )
    }
  }

  /** Выключить мониторинг изменения состояния. */
  override def onChangeReset(): Unit =
    pStatus.onchange = null

}


/** Реализация [[IPermissionApi]] для определения прав на геолокацию. */
class Html5GeoLocPermissionApi extends IPermissionApi {

  /** Доступ к геолокации разрешён? */
  override def getPermissionState(): Future[Html5PermissionData] =
    Html5PermissionApi.getGeoLocState( PermissionName.geolocation )

}
