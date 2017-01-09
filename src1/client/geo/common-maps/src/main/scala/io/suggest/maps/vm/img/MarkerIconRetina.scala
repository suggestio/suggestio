package io.suggest.maps.vm.img

import io.suggest.common.maps.MapFormConstants
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 21:59
 * Description: vm'ка для доступа к картинке retina-маркера.
 */
object MarkerIconRetina extends IconVmStaticT {
  override def DOM_ID = MapFormConstants.IMG_ID_MARKER_RETINA
  override type T     = MarkerIconRetina
}


case class MarkerIconRetina(
  override val _underlying: HTMLImageElement
)
  extends IconVmT
