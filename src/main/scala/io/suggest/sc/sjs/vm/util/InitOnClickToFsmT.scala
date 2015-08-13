package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmEventMsgCompanion}
import io.suggest.sc.sjs.v.vutil.OnClickSelfT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:49
 * Description: Аддон для поддержки навешивания onClick-листенера при инициализации.
 * Подмешивается к экземплярам кнопок.
 */
trait SendEventToFsmUtil {

  protected def _sendEventF[EventT <: Event](model: IFsmMsgCompanion[EventT]) = {
    {e: EventT =>
      ScFsm ! model(e)
    }
  }

}

// TODO Надо наверное спилить этот трейт во имя более универсального варианта.
trait InitOnClickToFsmT extends IInitLayout with OnClickSelfT with SafeEventTargetT with SendEventToFsmUtil {

  /** Статический компаньон модели для сборки сообщений. */
  protected[this] def _clickMsgModel: IFsmEventMsgCompanion

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    val f = _sendEventF[Event](_clickMsgModel)
    onClick(f)
  }
}


/** Быстрая вешалка listener'ов DOM-событий на элемент. Подмешивается к vm-классам. */
trait InitOnEventToFsmUtilT extends SafeEventTargetT with SendEventToFsmUtil {
  protected def _addToFsmEventListener[EventT <: Event](eventType: String, model: IFsmMsgCompanion[EventT]): Unit = {
    val f = _sendEventF[EventT](model)
    addEventListener(eventType)(f)
  }
}

