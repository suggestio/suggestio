package io.suggest.sc.sjs.vm.search.tabs.geo

import io.suggest.sc.sjs.m.msearch.STabBtnGeoClick
import io.suggest.sc.sjs.vm.search.tabs.{TabBtn, TabBtnCompanion}
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.MapTab.TAB_BTN_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 17:44
 * Description: vm кнопки таба геопоиска на панели поиска.
 */
object SGeoTabBtn extends TabBtnCompanion {
  override type T = SGeoTabBtn
  override def DOM_ID = TAB_BTN_ID
}


trait SGeoTabBtnT extends TabBtn {
  override protected[this] def _clickMsgModel = STabBtnGeoClick
}


case class SGeoTabBtn(
  override val _underlying: HTMLDivElement
)
  extends SGeoTabBtnT
