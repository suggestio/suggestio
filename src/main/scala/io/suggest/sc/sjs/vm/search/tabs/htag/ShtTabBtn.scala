package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.sjs.m.msearch.STabBtnHtagsClick
import io.suggest.sc.sjs.vm.search.tabs.{TabBtnCompanion, TabBtn}
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Nodes.TAB_BTN_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:07
 * Description: Кнопка таба хештегов на панели поиска.
 */
object ShtTabBtn extends TabBtnCompanion {

  override type T = ShtTabBtn

  override def DOM_ID = TAB_BTN_ID

}


/** Логика экземпляра vm вынесена в этот трейт. */
trait ShtTabBtnT extends TabBtn {
  override protected[this] def _clickMsgModel = STabBtnHtagsClick
}


case class ShtTabBtn(
  override val _underlying: HTMLDivElement
)
  extends ShtTabBtnT
