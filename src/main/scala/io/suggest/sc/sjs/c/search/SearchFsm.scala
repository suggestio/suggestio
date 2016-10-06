package io.suggest.sc.sjs.c.search

import io.suggest.sc.sjs.c.search.map.MapFsm
import io.suggest.sc.sjs.c.search.tags.TagsFsm
import io.suggest.sc.sjs.m.msearch.{MSearchFsmSd, MTab, MTabs}
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:37
  * Description: FSM обслуживания панели поиска и её FTS-поля.
  * Для вкладок панели поиска используются изолированные FSM, подчиненные этому FSM.
  */
object SearchFsm
  extends SjsFsmImpl
  with OnSearch
  with ScSjsFsmLogger
  //with LogBecome
{

  override protected var _stateData: SD  = {
    MSearchFsmSd(
      currTab = null
    )
  }
  override protected var _state: State_t = new DummyState

  private class DummyState extends FsmState with FsmEmptyReceiverState

  /** Запуск этого FSM на исполнение. */
  def start(): Unit = {
    // Собираем начальное состояние
    val sd1 = _stateData.copy(
      // TODO Определять текущую видимую вкладку из DOM
      currTab = MTabs.values.head,

      // Инициализировать FSM-табы
      tabs = {
        MTabs.values
          .iterator
          .map { mtab =>
            val fac = mtab2fsmFactory(mtab)
            val fsm = fac.apply()
            fsm.start()
            (mtab, fsm)
          }
          .toMap
      }
    )

    become( new OnSearchState, sd1 )
  }


  /** Доступ к tab fsm factory для указанного таба. */
  protected def mtab2fsmFactory(mtab: MTab): ITabFsmFactory = {
    mtab match {
      case MTabs.Geo  => MapFsm
      case MTabs.Tags => TagsFsm
    }
  }


  // States

  class OnSearchState extends OnSearchStateT


  // Public API

  /** Доступ к FSM тегов, если он запущен. */
  def tagsFsm = _subFsm( MTabs.Tags )

  /** Доступ к FSM карты, если он запущен. */
  def mapFsm  = _subFsm( MTabs.Geo )

}
