package io.suggest.sc.sjs.vm.hdr.btns

import io.suggest.sc.ScConstants.Header.SHOW_INDEX_BTN_ID
import io.suggest.sc.sjs.m.mhdr.ShowIndexClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:27
 * Description: ViewModel кнопки отображения index'а (плитки) узла.
 */
object HShowIndexBtn extends FindDiv {

  override type T = HShowIndexBtn

  override def DOM_ID = SHOW_INDEX_BTN_ID

}


/** Трейт с логикой ViewModel'и кнопки отображения index'а выдачи. */
trait HShowIndexBtnT extends InitOnClickToFsmT {

  override type T = HTMLDivElement

  override protected[this] def _clickMsgModel = ShowIndexClick

}


/** Дефолтовая реализация ViewModel'и кнопки отображения index'а. */
case class HShowIndexBtn(
  override val _underlying: HTMLDivElement
)
  extends HShowIndexBtnT
