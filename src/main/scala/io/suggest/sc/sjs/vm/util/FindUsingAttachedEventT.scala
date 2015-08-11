package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.vm.util.domvm.{IFindEl, IApplyEl}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 18:18
 * Description: Поиск экземпляра модели из события.
 * Этакая оптимизация, может быть небезопасной (см. TODO ниже).
 */
trait FindViaAttachedEventT extends IApplyEl {

  /** Найти экземпляр модели с использованием события, произошедшего в listener, повешенном тут же. */
  def findUsingEvent(event: Event): Option[T] = {
    Option( event.currentTarget )
      // TODO Фильтровать по типу тега Dom_t надо бы...
      .orElse { Option(event.target) }
      .map { el => apply( el.asInstanceOf[Dom_t] ) }
  }

}


/** Поиск экземпляра модели со статическим id с использованием доступного события. */
trait FindUsingAttachedEventT extends FindViaAttachedEventT with IFindEl {

  override def findUsingEvent(event: Event): Option[T] = {
    super.findUsingEvent(event)
      .orElse { find() }
  }

}
