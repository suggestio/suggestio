package io.suggest.lk.tags.edit.vm.util

import io.suggest.lk.tags.edit.fsm.TagsEditFsm
import io.suggest.sjs.common.fsm.{InitOnClickToFsmT, SendEventToFsmUtil, OnEventToFsmUtilT}
import io.suggest.sjs.common.vm.evtg.OnMouseClickT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 11:47
 * Description: Утиль для сборки vm'ок, связанных с tags fsm.
 */
trait ToTagsEditFsm extends SendEventToFsmUtil {
  override protected def FSM = TagsEditFsm
}


trait OnEventToTagsEditFsmUtilT
  extends OnEventToFsmUtilT
  with ToTagsEditFsm


trait InitOnClickToTagsEditFsmT
  extends InitOnClickToFsmT
  with OnMouseClickT
  with ToTagsEditFsm
{
  override protected def isTouchLocked = false
}
