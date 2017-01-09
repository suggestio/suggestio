package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_BTN_ID
import io.suggest.lk.tags.edit.m.signals.AddBtnClick
import io.suggest.sjs.common.fsm.InitLayoutFsmClickT
import io.suggest.sjs.common.vm.evtg.ClickPreventDefault
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:34
 * Description: vm'ка кнопки добавления тега.
 */
object AddBtn extends FindElT {
  override type Dom_t = HTMLInputElement
  override type T     = AddBtn
  override def DOM_ID = ADD_BTN_ID
}


/** Логика экземпляров vm'ки описана здесь. */
trait AddBtnT extends InitLayoutFsmClickT with ClickPreventDefault {

  override type T = HTMLInputElement

  override protected[this] def _clickMsgModel = AddBtnClick

}


/** Дефолтовая реализация vm'ки этой. */
case class AddBtn(
  override val _underlying: HTMLInputElement
)
  extends AddBtnT
