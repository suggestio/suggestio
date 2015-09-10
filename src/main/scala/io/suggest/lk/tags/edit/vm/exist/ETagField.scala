package io.suggest.lk.tags.edit.vm.exist

import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 15:30
 * Description: vm adn-тега для элемента input type=hidden.
 */
object ETagField extends IApplyEl {

  override type T = ETagField
  override type Dom_t = HTMLInputElement

  def TAG_NAME = "input"

}

trait ETagFieldT extends IVm {

  override type T = HTMLInputElement

}


case class ETagField(
  override val _underlying: HTMLInputElement
)
  extends ETagFieldT
