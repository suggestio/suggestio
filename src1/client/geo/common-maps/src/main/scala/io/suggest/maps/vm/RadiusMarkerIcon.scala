package io.suggest.maps.vm

import io.suggest.common.maps.rad.RadConst
import io.suggest.maps.vm.img.{IconVmStaticT, IconVmT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.01.17 16:35
  * Description: vm'ка картинки с данными для маркера радиуса круга охвата.
  */

object RadiusMarkerIcon extends IconVmStaticT {
  override type T     = RadiusMarkerIcon
  override def DOM_ID = RadConst.IMG_ID_RADIUS_MARKER
}


import io.suggest.maps.vm.RadiusMarkerIcon.Dom_t


case class RadiusMarkerIcon(
  override val _underlying: Dom_t
)
  extends IconVmT
