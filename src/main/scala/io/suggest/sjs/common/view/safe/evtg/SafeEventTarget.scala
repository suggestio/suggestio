package io.suggest.sjs.common.view.safe.evtg

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom
import org.scalajs.dom.{Event, EventTarget}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 16:20
 * Description: Враппер над EventTarget, дающий кросс-браузерный доступ к addEventListener()/attachEvent().
 * Оно нужно только для IE8 и ранее, поэтому потом можно удалить.
 * @see [[http://www.w3schools.com/jsref/met_document_addeventlistener.asp]]
 */
object SafeEventTarget {

  /** Интерфейс плагина для вешанья событий. */
  sealed trait IFacade {
    def addEventListener[T <: Event](eventTarget: EventTarget, eventType: String, listener: js.Function1[T,_]): Unit
  }
  /** Standard-compilant вешалка событий. */
  private class StdFacade extends IFacade {
    override def addEventListener[T <: Event](eventTarget: EventTarget, eventType: String, listener: js.Function1[T, _]): Unit = {
      eventTarget.addEventListener(eventType, listener)
    }
  }
  /** Вешалка событий на старые IE. */
  private class IeFacade extends IFacade {
    override def addEventListener[T <: Event](eventTarget: EventTarget, eventType: String, listener: js.Function1[T, _]): Unit = {
      val ae = AttachEventStub(eventTarget)
      ae.attachEvent[T]("on" + eventType, listener)
    }
  }

  /** Проверка и её результат сохраняется тут. */
  val FACADE: IFacade = {
    val e = SafeEventTargetStub( dom.document )
    val ael = e.addEventListener
    if (ael.isDefined) {
      new StdFacade
    } else if (e.attachEvent.isDefined) {
      new IeFacade
    } else {
      throw new UnsupportedOperationException("E01")
    }
  }

}


trait SafeEventTargetT extends ISafe {

  override type T <: EventTarget

  /**
   * Повесить событие на текущую цель.
   * @param eventType Тип события.
   * @param listener Функция-обработчик события.
   * @tparam T Тип события, передаваемого в listener.
   */
  def addEventListener[T <: Event](eventType: String)(listener: js.Function1[T, _]): Unit = {
    SafeEventTarget.FACADE
      .addEventListener(_underlying, eventType, listener)
  }

}


/** Дефолтовая реалзизация враппера [[SafeEventTargetT]]. */
// TODO Этот код не используется.
case class SafeEventTarget(override val _underlying: EventTarget)
  extends SafeEventTargetT {
  override type T = EventTarget
}


/** Безопасный интерфейс для доступа к DOM-элементу в области вешанья событий. */
@js.native
sealed trait SafeEventTargetStub extends js.Object {
  def addEventListener: UndefOr[_] = js.native
  def attachEvent: UndefOr[_] = js.native
}
object SafeEventTargetStub {
  def apply(e: EventTarget): SafeEventTargetStub = {
    e.asInstanceOf[SafeEventTargetStub]
  }
}


/** IE-костыли для навешивания событий. */
@js.native
sealed trait AttachEventStub extends js.Object {
  def attachEvent[T <: Event](eventType: String, listener: js.Function1[T,_]): Unit = js.native
}
object AttachEventStub {
  def apply(e: EventTarget): AttachEventStub = {
    e.asInstanceOf[AttachEventStub]
  }
}