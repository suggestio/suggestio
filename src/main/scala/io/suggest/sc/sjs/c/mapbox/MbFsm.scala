package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mmap.MbFsmSd
import io.suggest.sjs.common.util.SjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:37
  * Description: MapBox FSM: система обслуживания карты, начиная с фоновой инициализации.
  *
  * Весь процесс асинхронен и независим от ScFsm, кроме финального уведомления ScFsm об итогах инициализации.
  * Скрипт mapbox грузиться асинхронно, поэтому его надо поджидать с использованием таймера.
  */
object MbFsm
  extends AwaitMbglJs
  with MbgljsReady
  with SjsLogger
{

  override protected var _stateData: SD = MbFsmSd()
  override protected var _state: State_t = new DummyState

  private class DummyState extends FsmState with FsmEmptyReceiverState


  /** Запуск этого FSM на исполнение. */
  def start(): Unit = {
    become(new AwaitMbglJsState)
  }


  // -- states impl
  // Состояние ожидания и начальной инициализации карты
  class AwaitMbglJsState extends AwaitMbglJsStateT {
    override def jsReadyState: State_t = new StandByState
  }

  // Карта инициализирована.
  class StandByState extends MbgljsReadyStateT

}
