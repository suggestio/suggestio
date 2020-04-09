package io.suggest.os.notify.api.html5

import io.suggest.perm.{BoolOptPermissionState, Html5PermissionApi, IPermissionState}
import org.scalajs.dom.experimental.Notification

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.04.2020 18:17
  * Description: Утиль для HTML5 Notification API.
  */
object Html5NotificationUtil {

  /** Тест доступности API. */
  def isApiAvailable(): Boolean =
    Try( Notification.permission ).isSuccess


  def getPermissionState(): Future[IPermissionState] = {
    val res = BoolOptPermissionState(
      permitted = Html5PermissionApi.parsePermissionValue( Notification.permission ),
    )
    Future.successful(res)
  }

}
