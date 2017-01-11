package io.suggest.lk.adv.geo.vm.rad

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.maps.vm.img.{IconVmStaticT, IconVmT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.01.17 16:35
  * Description: vm'ка картинки с данными для маркера радиуса круга охвата.
  */

object RadiusMarkerIcon extends IconVmStaticT {
  override type T     = RadiusMarkerIcon
  override def DOM_ID = AdvGeoConstants.Rad.IMG_ID_RADIUS_MARKER
}


import RadiusMarkerIcon.Dom_t


case class RadiusMarkerIcon(
  override val _underlying: Dom_t
)
  extends IconVmT
