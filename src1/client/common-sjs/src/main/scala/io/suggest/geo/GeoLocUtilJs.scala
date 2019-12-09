package io.suggest.geo

import io.suggest.sjs.common.vm.wnd.WindowVm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.2019 22:41
  * Description: Утиль для работы геолокации на клиенте.
  */
object GeoLocUtilJs {

  /** Девайс имеет гео-локацию? */
  def envHasGeoLoc(): Boolean = {
    WindowVm()
      .navigator
      .exists( _.geolocation.nonEmpty )
  }

}
