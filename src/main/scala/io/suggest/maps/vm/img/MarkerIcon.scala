package io.suggest.maps.vm.img

import io.suggest.common.maps.MapFormConstants
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 19:21
 * Description: VM'ка для получения основной иконки маркера.
 */
object MarkerIcon extends IconVmStaticT {
  override def DOM_ID = MapFormConstants.IMG_ID_MARKER
  override type T     = MarkerIcon
}


case class MarkerIcon(
  override val _underlying: HTMLImageElement
)
  extends IconVmT
