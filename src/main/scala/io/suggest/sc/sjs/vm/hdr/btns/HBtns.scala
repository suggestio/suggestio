package io.suggest.sc.sjs.vm.hdr.btns

import io.suggest.sc.sjs.vm.util.domvm.FindSpan
import io.suggest.sc.ScConstants.Header.BTNS_DIV_ID
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.HTMLSpanElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 18:25
 * Description: VM контейнера кнопок заголовка.
 * Так получилось, что кнопка сокрытия навигации живёт отдельно от кнопок заголовка.
 * И кнопки заголовка бывает необходимо резко скрывать.
 */
object HBtns extends FindSpan {

  override type T = HBtns

  override def DOM_ID = BTNS_DIV_ID

}


trait HBtnsT extends SetDisplayEl {

  override type T = HTMLSpanElement


  /** Скрыть все базовые кнопки строки заголовка. */
  def hide(): Unit = {
    displayNone()
  }

  /** Показать базовые кнопки строки заголовка. */
  def show(): Unit = {
    displayBlock()
  }

}


case class HBtns(
  override val _underlying: HTMLSpanElement
)
  extends HBtnsT
