package io.suggest.lk.tags.edit.fsm

import io.suggest.lk.tags.edit.m.MStateData
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:35
 * Description: FSM, обслуживающий подсистему редактирования тегов в формах.
 */
object TagsEditFsm extends TagsEditFsmStub with SjsLogger {

  // Инициализация начальных значений состояния.
  override protected var _stateData: SD = MStateData()
  override protected var _state: State_t = ???

}
