package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sc.sjs.vm.search.tabs.htag.ShtTabBtn
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Search.TAB_BTNS_DIV_ID
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 13:44
 * Description: VM контейнера кнопок вкладок.
 */
object STabsHeader extends FindDiv {

  override type T = STabsHeader

  override def DOM_ID: String = TAB_BTNS_DIV_ID

}


trait STabBtnsT extends SafeElT {

  override type T = HTMLDivElement

  def htagsBtn = ShtTabBtn.find()

  def btns = List(htagsBtn)

}


case class STabsHeader(
  override val _underlying: HTMLDivElement
)
  extends STabBtnsT
