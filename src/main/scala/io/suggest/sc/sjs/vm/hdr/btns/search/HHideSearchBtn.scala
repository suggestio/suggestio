package io.suggest.sc.sjs.vm.hdr.btns.search

import io.suggest.sc.ScConstants.Search.HIDE_PANEL_BTN_ID
import io.suggest.sc.sjs.m.mhdr.HideSearchClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:05
 * Description: ViewModel кнопки сокрытия панели поиска.
 */
object HHideSearchBtn extends FindDiv {

  override type T = HHideSearchBtn

  override def DOM_ID: String = HIDE_PANEL_BTN_ID

}


/** Логика div'а кнопки сокрытия вынесена сюда. */
trait HHideSearchBtnT extends InitOnClickToFsmT {

  override type T = HTMLDivElement

  override protected[this] def _clickMsgModel = HideSearchClick

}


/** Дефолтовая реализация ViewModel'и кнопки сокрытия панели поиска.
  * @param _underlying Корневой DOM div элемент кнопки.
  */
case class HHideSearchBtn(
  override val _underlying: HTMLDivElement
)
  extends HHideSearchBtnT
