package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.fsm.{OnEventToFsmUtilT, InitOnClickToFsmT, SendEventToFsmUtil}
import io.suggest.sjs.common.vm.evtg.OnClickT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:49
 * Description: Sc-реализации различных vm->fsm аддонов. Подмешиваются к экземплярам vm.
 */
trait SendEventToScFsmUtil extends SendEventToFsmUtil {
  override protected def FSM = ScFsm
}


/** Реализация common OnClickT в рамках sc. */
trait OnClick extends OnClickT {
  override protected def isTouchLocked = MTouchLock()
}


// TODO Надо наверное спилить этот трейт во имя более универсального варианта.
trait InitOnClickToScFsmT
  extends InitOnClickToFsmT
  with OnClick
  with SendEventToScFsmUtil


/** Быстрая вешалка listener'ов DOM-событий на элемент. Подмешивается к vm-классам. */
trait OnEventToScFsmUtilT
  extends OnEventToFsmUtilT
  with SendEventToScFsmUtil
