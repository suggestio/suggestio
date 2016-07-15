package io.suggest.sc.sjs.c.search

import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.m.mgeo.{Subscribe, SubscriberData}
import io.suggest.sc.sjs.m.mmap.MbFsmSd
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:37
  * Description: FSM обслуживания панели поиска и её элементов: карты, тегов, FTS.
  *
  * Изначально (до 2016.jul.15) это была система обслуживания только карты, начиная с её фоновой инициализации.
  * (название MbFsm = MapBox FSM).
  * Усиление влияния MbFsm произошло из-за необходимости прозрачного объединения карты, геотегов,
  * текстового поиска и остальных комплексных функций панели поиска.
  */
object SearchFsm
  extends SjsFsmImpl
  with map.Phase
  with ScSjsFsmLogger
  //with LogBecome
{

  override protected var _stateData: SD = MbFsmSd()
  override protected var _state: State_t = new DummyState

  private class DummyState extends FsmState with FsmEmptyReceiverState


  /** Запуск этого FSM на исполнение. */
  def start(): Unit = {
    become(new MapAwaitJsState)

    // Подписаться на события геолокации текущего юзера.
    GeoLocFsm ! Subscribe(
      receiver    = this,
      notifyZero  = true,
      data        = SubscriberData(
        withErrors = false
      )
    )
  }


}
