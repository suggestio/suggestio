package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_NAME_INPUT_ID
import io.suggest.lk.tags.edit.m.signals._
import io.suggest.sjs.common.fsm.{SendEventToFsmUtil, IInitLayoutFsm, OnEventToArgFsmUtilT, SjsFsm}
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.of.OfEventTargetNode
import io.suggest.sjs.common.vm.walk.Focus
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 11:54
 * Description: VM'ка для работы с полем ввода имени тега.
 * Далее происходит поиск и добавление тега.
 */
object ANameInput extends FindElT with OfEventTargetNode {

  override type Dom_t = HTMLInputElement
  override type T     = ANameInput
  override def DOM_ID = ADD_NAME_INPUT_ID

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    el.id == DOM_ID
  }

}


trait ANameInputT extends IInitLayoutFsm with OnEventToArgFsmUtilT with Focus with StringInputValueT {

  override type T = HTMLInputElement

  override def initLayout(fsm: SjsFsm): Unit = {
    // Вешаем проброску событий ввода и фокуса в FSM.
    //_addToFsmEventListener(fsm, "focus", NameInputFocus)
    //_addToFsmEventListener(fsm, "blur",  NameInputBlur)
    //_addToFsmEventListener(fsm, "input", NameInputEvent)
    _addToFsmEventListener(fsm, "keyup", TagNameTyping)

    // Нужно реагировать на некоторые клавиши, как на сабмит.
    val f = SendEventToFsmUtil.f(fsm, NameInputSubmit)
    addEventListener("keyup") { e: KeyboardEvent =>
      if (e.keyCode == KeyCode.Enter)
        f(e)
    }
  }

}


case class ANameInput(
  override val _underlying: HTMLInputElement
)
  extends ANameInputT
