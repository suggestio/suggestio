package io.suggest.sc.sjs.vm.hdr.btns

import io.suggest.sc.ScConstants.Header.{PREV_NODE_BTN_ID, ATTR_ADN_ID}
import io.suggest.sc.sjs.m.mhdr.PrevNodeBtnClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.15 17:27
 * Description: vm кнопки возврата на предыдущий узел.
 * Кнопка рендерится сервером, когда текущий узел не участвует в геолокации.
 */
object HNodePrev extends FindDiv {

  override type T = HNodePrev
  override def DOM_ID = PREV_NODE_BTN_ID

}


trait HNodePrevT extends SafeElT with InitOnClickToFsmT {

  override protected[this] def _clickMsgModel = PrevNodeBtnClick

  override type T = HTMLDivElement

  def adnId = getAttribute(ATTR_ADN_ID)

}


case class HNodePrev(
  override val _underlying: HTMLDivElement
)
  extends HNodePrevT
