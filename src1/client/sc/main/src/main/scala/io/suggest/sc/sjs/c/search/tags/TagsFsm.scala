package io.suggest.sc.sjs.c.search.tags

import io.suggest.primo.IStart0
import io.suggest.sc.sjs.c.search.ITabFsmFactory
import io.suggest.sc.sjs.m.mtags.MTagsSd
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 15:05
  * Description: FSM-контроллер, занимающийся списком тегов на панели поиска.
  */
object TagsFsm extends ITabFsmFactory {
  override type T = TagsFsm
}

case class TagsFsm()
  extends SjsFsmImpl
  with IStart0
  with Hidden
  with OnTags
{

  override protected var _stateData: MTagsSd = MTagsSd()
  override protected var _state: FsmState = new DummyState

  private class DummyState extends FsmEmptyReceiverState


  /** Запуск этого FSM. */
  override def start(): Unit = {
    // TODO Определять текущее состояние на основе видимости
    become(new HiddenState)
  }


  // States

  class HiddenState extends HiddenStateT {
    override def visibleState = new OnTagsState
  }

  class OnTagsState extends OnTagsStateT {
    override def _hiddenState = new HiddenState
  }

}
