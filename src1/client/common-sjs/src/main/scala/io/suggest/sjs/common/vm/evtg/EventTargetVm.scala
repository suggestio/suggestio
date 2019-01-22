package io.suggest.sjs.common.vm.evtg

import io.suggest.sjs.common.vm.IVm
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
object EventTargetVm {

  /** Интерфейс плагина для вешанья событий. */
  sealed trait IFacade {
    def addEventListener(eventTarget: EventTarget, eventType: String, listener: js.Function): Unit
  }
  /** Standard-compilant вешалка событий. */
  private class StdFacade extends IFacade {
    override def addEventListener(eventTarget: EventTarget, eventType: String, listener: js.Function): Unit =
      eventTarget.addEventListener(eventType, listener.asInstanceOf[js.Function1[_, _]])
  }
  /** Вешалка событий на старые IE. */
  private class IeFacade extends IFacade {
    override def addEventListener(eventTarget: EventTarget, eventType: String, listener: js.Function): Unit = {
      val ae = AttachEventStub(eventTarget)
      ae.attachEvent("on" + eventType, listener)
    }
  }

  /** Проверка и её результат сохраняется тут. */
  private val FACADE: IFacade = {
    if (
      // ServiceWorker не имеет ни window, ни window.document в scope.
      !js.isUndefined(dom.window) &&
      !js.isUndefined(dom.document) &&
      SafeEventTargetStub( dom.document ).attachEvent.isDefined
    ) {
      new IeFacade
    } else {
      new StdFacade
    }
  }


  /** AnyVal-экстеншены для EventTarget, чтобы не мутить новые vm-классы,
    * а просто юзать scala-синтаксис без лишнего гемора. */
  implicit class RichEventTarget(val underlying: EventTarget) extends AnyVal {
    def addEventListener4s[T <: Event](eventType: String)(listener: (T) => _): Unit =
      FACADE.addEventListener(underlying, eventType, listener)
    def addEventListenerThis[This, T <: Event](eventType: String)(listener: (This, T) => _): Unit =
      FACADE.addEventListener(underlying, eventType, listener: js.ThisFunction)
  }

}


trait EventTargetVmT extends IVm {

  override type T <: EventTarget

  /**
   * Повесить событие на текущую цель.
   * @param eventType Тип события.
   * @param listener Функция-обработчик события.
   * @tparam T Тип события, передаваемого в listener.
   */
  def addEventListener[T <: Event](eventType: String)(listener: (T) => _): Unit = {
    import EventTargetVm._
    _underlying.addEventListener4s(eventType)(listener)
  }

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
  def attachEvent(eventType: String, listener: js.Function): Unit = js.native
}
object AttachEventStub {
  def apply(e: EventTarget): AttachEventStub = {
    e.asInstanceOf[AttachEventStub]
  }
}