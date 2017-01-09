package io.suggest.lk.tags.edit.fsm

import io.suggest.lk.tags.edit.fsm.states.{Add, StandBy}
import io.suggest.lk.tags.edit.m.MStateData
import io.suggest.lk.tags.edit.vm.TVm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:35
 * Description: FSM, обслуживающий подсистему редактирования тегов в формах.
 */
abstract class TagsEditFsm extends StandBy with Add {

  // Инициализация начальных значений состояния.
  override protected var _stateData: SD   = MStateData()
  override protected var _state: State_t  = new DummyState

  def start(): Unit = {
    TVm.initLayout(this)
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
