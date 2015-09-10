package io.suggest.lk.tags.edit.fsm

import io.suggest.lk.tags.edit.fsm.states.{StandBy, Add}
import io.suggest.lk.tags.edit.m.MStateData
import io.suggest.lk.tags.edit.vm.TVm
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:35
 * Description: FSM, обслуживающий подсистему редактирования тегов в формах.
 */
object TagsEditFsm extends StandBy with Add with SjsLogger {

  // Инициализация начальных значений состояния.
  override protected var _stateData: SD   = MStateData()
  override protected var _state: State_t  = new DummyState

  def start(): Unit = {
    TVm.initLayout()
    become(new SimpleStandByState)
  }

  /** Заглушка для начального состояния.*/
  private class DummyState extends FsmEmptyReceiverState


  class SimpleStandByState extends SimpleStandByStateT {
    override protected def _addBtnClickedState  = new AddBtnClickedState
  }

  class AddBtnClickedState extends AddClickedInitT with AddRespStateT {
    override protected def _allDoneState        = new SimpleStandByState
  }

}
