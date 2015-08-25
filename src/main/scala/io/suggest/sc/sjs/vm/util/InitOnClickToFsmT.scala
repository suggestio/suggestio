package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmEventMsgCompanion}
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import io.suggest.sjs.common.view.vutil.OnClickT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:49
 * Description: Аддон для поддержки навешивания onClick-листенера при инициализации.
 * Подмешивается к экземплярам кнопок.
 */
trait SendEventToFsmUtil {

  /**
   * Сборка фунцкии для отправки DOM-события в ScFsm, заворачивая его в соотв.контейнер.
   * @param model Компаньон модели контейнера.
   * @tparam EventT Тип заворачиваемого события.
   * @return Функция, пригодная для повешивания в качестве листенера.
   */
  protected def _sendEventF[EventT](model: IFsmMsgCompanion[EventT]) = {
    {e: EventT =>
      ScFsm !! model(e)
    }
  }

}


/** Реализация common OnClickT в рамках sc. */
trait OnClick extends OnClickT {

  override protected def isTouchLocked = MTouchLock()
}


// TODO Надо наверное спилить этот трейт во имя более универсального варианта.
trait InitOnClickToFsmT extends IInitLayout with OnClick with SafeEventTargetT with SendEventToFsmUtil {

  /** Статический компаньон модели для сборки сообщений. */
  protected[this] def _clickMsgModel: IFsmEventMsgCompanion

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  def initLayout(): Unit = {
    val f = _sendEventF[Event](_clickMsgModel)
    onClick(f)
  }
}


/** Быстрая вешалка listener'ов DOM-событий на элемент. Подмешивается к vm-классам. */
trait OnEventToFsmUtilT extends SafeEventTargetT with SendEventToFsmUtil {
  protected def _addToFsmEventListener[Event_t <: Event](eventType: String, model: IFsmMsgCompanion[Event_t]): Unit = {
    val f = _sendEventF[Event_t](model)
    addEventListener(eventType)(f)
  }
}
