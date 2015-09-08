package io.suggest.lk.tags.edit.fsm.states

import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.madd.MTagAdd
import io.suggest.lk.tags.edit.m.signals.AddBtnClick
import io.suggest.lk.tags.edit.vm.add.{Container, NameInput}
import org.scalajs.dom.raw.FormData
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:29
 * Description: Аддон для [[io.suggest.lk.tags.edit.fsm.TagsEditFsm]] для реализации примитивной
 * работы без участия сервера.
 * Создан как заглушка во избежание написания кучи кода на стороне сервера и на клиенте.
 */
trait Simple extends TagsEditFsmStub {

  protected trait SimpleOperationStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по кнопке добавления тега.
      case AddBtnClick(event) =>
        _addBtnClicked()
    }

    /** Реакция на клик по кнопке добавления тега. */
    protected def _addBtnClicked(): Unit = {
      for (input <- NameInput.find() if !input.value.isEmpty) {
        val fd = new FormData()
        fd.append(NameInput.DOM_ID, input.value)

        val index = 1   // TODO Определять порядковый номер на странице.

        MTagAdd.add(index, fd) foreach {
          // Тег можно добавлять на экран.
          case Right(tagHtml) =>
            ???   // TODO Добавить в контейнер тегов новый тег, почистить форму.

          // Нужно перезаписать форму ввода тега.
          case Left(formHtml) =>
            for (cont <- Container.find()) {
              val cont2 = Container(formHtml)
              cont.replaceWith(cont2)
            }
        }
      }
    }

  }

}
