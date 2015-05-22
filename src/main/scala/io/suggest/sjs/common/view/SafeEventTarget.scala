package io.suggest.sjs.common.view

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
trait SafeEventTargetT {

  /** wrapped-элемент, для которого реализуется безопасный доступ. */
  def _underlying: EventTarget

  /**
   * Повесить событие на текущую цель.
   * @param eventType Тип события.
   * @param listener Функция-обработчик события.
   * @tparam T Тип события, передаваемого в listener.
   */
  def addEventListener[T <: Event](eventType: String)(listener: js.Function1[T, _]): Unit = {
    val e = _underlying.asInstanceOf[SafeEventTargetStub]
    val ael = e.addEventListener
    if (ael.isDefined) {
      _underlying.addEventListener(eventType, listener)
    } else {
      val ae = e.attachEvent[T]
      if (ae.isDefined) {
        ae.get("on" + eventType, listener)
      }
    }
  }

}


/** Дефолтовая реалзизация враппера [[SafeEventTargetT]]. */
case class SafeEventTarget(override val _underlying: EventTarget)
  extends SafeEventTargetT


/** Безопасный интерфейс для доступа к DOM-элементу в области вешанья событий. */
sealed trait SafeEventTargetStub extends js.Object {

  def addEventListener: UndefOr[_] = js.native

  def attachEvent[T <: Event]: UndefOr[js.Function2[String, js.Function1[T,_], Unit]] = js.native

}
