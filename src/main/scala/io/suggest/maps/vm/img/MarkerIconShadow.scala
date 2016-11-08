package io.suggest.maps.vm.img

import io.suggest.common.maps.MapFormConstants
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 22:00
 * Description: vm'ка для доступа к тешу с тенью маркера.
 */
object MarkerIconShadow extends IconVmStaticT {
  override def DOM_ID = MapFormConstants.IMG_ID_MARKER_SHADOW
  override type T     = MarkerIconShadow
}


case class MarkerIconShadow(
  override val _underlying: HTMLImageElement
)
  extends IconVmT
