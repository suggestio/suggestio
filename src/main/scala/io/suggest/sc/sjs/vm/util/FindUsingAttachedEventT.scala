package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.m.mfsm.CurrentTargetBackup
import io.suggest.sc.sjs.vm.util.domvm.{IFindEl, IApplyEl}
import org.scalajs.dom.EventTarget

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 18:18
 * Description: Поиск экземпляра модели из события.
 * Этакая оптимизация, может быть небезопасной (см. TODO ниже).
 */
trait FindViaAttachedEventT extends IApplyEl {

  /** Найти экземпляр модели с использованием события, произошедшего в listener, повешенном тут же. */
  def findUsingTargets(targets: EventTarget*): Option[T] = {
    targets
      .find { _ != null }
      .map { el => apply( el.asInstanceOf[Dom_t] ) }
  }
  
  def findUsing(e: CurrentTargetBackup): Option[T] = {
    findUsingTargets(e.currentTarget, e.event.target)
  }

}


/** Поиск экземпляра модели со статическим id с использованием доступного события. */
trait FindUsingAttachedEventT extends FindViaAttachedEventT with IFindEl {

  override def findUsingTargets(targets: EventTarget*): Option[T] = {
    super.findUsingTargets(targets : _*)
      .orElse { find() }
  }

}
