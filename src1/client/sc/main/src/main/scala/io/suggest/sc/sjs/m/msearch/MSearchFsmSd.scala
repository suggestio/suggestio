package io.suggest.sc.sjs.m.msearch

import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:13
  * Description: Модель состояния FSM, обслуживающего карту выдачи.
  * @param currTab Текущая активная вкладка панели поиска.
  * @param tabs Рантаймовая карта данных по всем имеющимся вкладкам.
  * @param fts Состояние полнотекстового поиска, если он активен.
  */
case class MSearchFsmSd(
  currTab       : MTab,
  tabs          : Map[MTab, SjsFsm]       = Map.empty,
  fts           : Option[MFtsFsmState]    = None
)
