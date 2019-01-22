package io.suggest.perm

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:06
  * Description: Интерфейс для получения инфы по пермишшенам.
  */
trait IPermissionsApi {

  /** Доступно ли данное API вообще? */
  def isApiAvailable(): Boolean

  /** Доступ к геолокации разрешён? */
  def isGeoLocationAuthorized(): Future[IPermissionData]

  /** Доступ к bluetooth разрешён? */
  def isBlueToothAuthorized(): Future[IPermissionData]

}

trait IPermissionData {

  def isGranted: Boolean

  def isDenied: Boolean

  def onChange(f: Boolean => _): Unit

}
