package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_NAME_INPUT_ID
import io.suggest.lk.tags.edit.m.signals._
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, OnEventToArgFsmUtilT, SendEventToFsmUtil, SjsFsm}
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.of.{OfEventTargetNode, OfInput}
import io.suggest.sjs.common.vm.util.OfHtmlElDomIdRelated
import io.suggest.sjs.common.vm.walk.Focus
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 11:54
 * Description: VM'ка для работы с полем ввода имени тега.
 * Далее происходит поиск и добавление тега.
 */
object ANameInput extends FindElT with OfEventTargetNode with OfInput with OfHtmlElDomIdRelated {

  override type Dom_t = HTMLInputElement
  override type T     = ANameInput
  override def DOM_ID = ADD_NAME_INPUT_ID

}

import ANameInput.Dom_t

trait ANameInputT extends IInitLayoutFsm with OnEventToArgFsmUtilT with Focus with StringInputValueT {

  override type T = Dom_t

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
  override val _underlying: Dom_t
)
  extends ANameInputT
