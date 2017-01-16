package io.suggest.lk.price.vm

import io.suggest.adv.AdvConstants.Price.Json.{GET_PRICE_URL_INPUT_ID, ATTR_METHOD}
import io.suggest.sjs.common.vm.attr.{AttrVmT, StringInputValueT}
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 11:23
 * Description: VM'ка, содержащая URL для рассчета стоимости с CSRF-защитой.
 */
object PriceUrlInput extends FindElT {
  override type Dom_t = HTMLInputElement
  override type T     = PriceUrlInput
  override def DOM_ID = GET_PRICE_URL_INPUT_ID
}


import io.suggest.lk.price.vm.PriceUrlInput.Dom_t


trait PriceUrlInputT extends StringInputValueT with SelfRemoveT with AttrVmT {

  override type T = Dom_t

  /** Прочитать HTTP-метод из соотв.аттрибута. */
  def method = getAttribute(ATTR_METHOD)

  /** Вернуть URL для запроса цены. */
  def url = value
}


case class PriceUrlInput(override val _underlying: Dom_t)
  extends PriceUrlInputT
