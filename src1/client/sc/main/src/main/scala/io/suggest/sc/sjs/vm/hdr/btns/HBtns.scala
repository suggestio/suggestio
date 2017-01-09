package io.suggest.sc.sjs.vm.hdr.btns

import io.suggest.sc.ScConstants.Header.BTNS_DIV_ID
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.raw.HTMLSpanElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 18:25
 * Description: VM контейнера кнопок заголовка.
 * Так получилось, что кнопка сокрытия навигации живёт отдельно от кнопок заголовка.
 * И кнопки заголовка бывает необходимо резко скрывать.
 */
object HBtns extends FindElT {
  override type Dom_t = HTMLSpanElement
  override type T = HBtns
  override def DOM_ID = BTNS_DIV_ID
}


trait HBtnsT extends ShowHideDisplayT {

  override type T = HTMLSpanElement

}


case class HBtns(
  override val _underlying: HTMLSpanElement
)
  extends HBtnsT
