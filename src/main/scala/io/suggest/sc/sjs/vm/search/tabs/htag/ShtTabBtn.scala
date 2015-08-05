package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.sjs.m.msearch.HtagsTabBtnClick
import io.suggest.sc.sjs.vm.search.tabs.TabBtn
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Nodes.TAB_BTN_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:07
 * Description: Кнопка таба хештегов на панели поиска.
 */
object ShtTabBtn extends FindDiv {

  override type T = ShtTabBtn

  override def DOM_ID = TAB_BTN_ID

}


/** Логика экземпляра vm вынесена в этот трейт. */
trait ShtTabBtnT extends TabBtn {

  override type T = HTMLDivElement

  override protected[this] def _msgModel = HtagsTabBtnClick

}


case class ShtTabBtn(
  override val _underlying: HTMLDivElement
)
  extends ShtTabBtnT
