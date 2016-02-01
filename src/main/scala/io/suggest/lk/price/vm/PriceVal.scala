package io.suggest.lk.price.vm

import io.suggest.adv.AdvConstants.Price
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 14:32
 * Description: VM'ка для вписывания рассчетной цены с сервера.
 */
object PriceVal extends FindElT {
  override type T       = PriceVal
  override type Dom_t   = HTMLElement
  override def DOM_ID   = Price.PRICE_INFO_CONT_ID
}


import io.suggest.lk.price.vm.PriceVal.Dom_t


trait PriceValT extends SetInnerHtml {
  override type T = Dom_t
}


case class PriceVal(override val _underlying: Dom_t)
  extends PriceValT
