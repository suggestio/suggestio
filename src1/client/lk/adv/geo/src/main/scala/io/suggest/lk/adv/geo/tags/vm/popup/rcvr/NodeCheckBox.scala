package io.suggest.lk.adv.geo.tags.vm.popup.rcvr

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.of.OfInputCheckBox
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 21:55
  * Description: VM'ка для чекбокса одного узла внутри попапа ресивера.
  */
object NodeCheckBox extends OfInputCheckBox {

  override type Dom_t = HTMLInputElement

  override type T = NodeCheckBox

}

import NodeCheckBox.Dom_t

case class NodeCheckBox(override val _underlying: Dom_t) extends VmT {

  override type T = Dom_t

  /** Доступ к родительскому контейнеру полей узла. */
  def nodeDiv: Option[NodeDiv] = {
    // TODO Использовать id'шники для надежной ориентации, а не этот ужас.
    Option( _underlying.parentElement.parentElement.parentElement )
      .flatMap( NodeDiv.ofHtmlEl )
  }

}
