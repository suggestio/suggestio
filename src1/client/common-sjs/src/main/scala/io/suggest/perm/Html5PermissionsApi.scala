package io.suggest.perm

import io.suggest.common.event.DomEvents
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom.experimental.permissions._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom.Event
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:13
  * Description: Доступ к Permissions API.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/Permissions_API]]
  */
class Html5PermissionsApi extends IPermissionsApi {

  /** Доступно ли данное API вообще? */
  override def isApiAvailable(): Boolean = {
    WindowVm()
      .navigator
      .flatMap(_.permissions)
      .nonEmpty
  }

  private def _do(name: PermissionName) = {
    DomQuick
      .askPermission( name )
      .map { Html5PermissionData.apply }
  }

  /** Доступ к геолокации разрешён? */
  override def isGeoLocationAuthorized(): Future[Html5PermissionData] = {
    _do( PermissionName.geolocation )
  }

  /** Доступ к bluetooth разрешён? */
  override def isBlueToothAuthorized(): Future[Html5PermissionData] =
    Future.failed( new NotImplementedException )

}

object Html5PermissionData {

  implicit class PermStatusExt( val pState: PermissionState ) extends AnyVal {
    def isGranted: Boolean =
      pState ==* PermissionState.granted
    def isDenied: Boolean =
      pState ==* PermissionState.denied
  }

}

case class Html5PermissionData(
                                pStatus: PermissionStatus
                              )
  extends IPermissionData
{
  import Html5PermissionData.PermStatusExt

  def isGranted: Boolean =
    pStatus.state.isGranted

  def isDenied: Boolean =
    pStatus.state.isDenied

  def onChange(f: Boolean => _): Unit = {
    pStatus.addEventListenerThis( DomEvents.CHANGE ) {
      (thisStatus: PermissionStatus, _: Event) =>
        f( thisStatus.state.isGranted )
    }
  }

}
