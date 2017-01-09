package io.suggest.lk.adv.geo.tags.vm

import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 18:38
  * Description: Map GeoJSON URL Input.
  * vm'ка для доступа к input'у ссылки на карту узлов.
  */

object AdIdInp extends FindElT {
  override def DOM_ID = AdvGeoConstants.AD_ID_INPUT_ID
  override type Dom_t = HTMLInputElement
  override type T = AdIdInp
}

import AdIdInp.Dom_t

case class AdIdInp(override val _underlying: Dom_t)
  extends StringInputValueT
  with SelfRemoveT
{
  override type T = Dom_t

  def adId = value

}
