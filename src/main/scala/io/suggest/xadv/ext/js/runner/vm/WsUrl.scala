package io.suggest.xadv.ext.js.runner.vm

import io.suggest.adv.ext.view.RunnerPage
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.09.15 11:46
 * Description: vm'ка для доступа к hidden-input, значение которого рендерится на сервере и
 * содержит ссылку для связи по вебсокету. Ссылка имеет qs с ЭЦП, поэтому нельзя через js-роутер это сделать.
 */
object WsUrl extends FindElT {
  override type Dom_t = HTMLInputElement
  override type T     = WsUrl
  override def DOM_ID = RunnerPage.ID_WS_URL
}


trait WsUrlT extends IVm with SelfRemoveT {

  override type T = HTMLInputElement

  def wsUrl = _underlying.value

}


case class WsUrl(
  override val _underlying: HTMLInputElement
)
  extends WsUrlT