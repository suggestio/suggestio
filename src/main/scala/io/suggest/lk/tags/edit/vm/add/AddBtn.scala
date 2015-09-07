package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants._
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.util.IInitLayoutDummy
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


/** Логика экземпляров vm'ки живёт здесь. */
trait AddBtnT extends IVm with IInitLayoutDummy {

  override type T = HTMLInputElement

  override def initLayout(): Unit = {
    super.initLayout()
    ???
  }
}


/** Дефолтовая реализация vm'ки этой. */
case class AddBtn(
  override val _underlying: HTMLInputElement
)
  extends AddBtnT
