package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.sjs.m.mgeo.MGeoLoc
import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:13
  * Description: Модель состояния FSM, обслуживающего карту выдачи.
  * @param mapInst Инстанс мапы.
  * @param lastUserLoc Последняя полученная геолокация юзера.
  * @param early Аккамулятор необработанных сообщений, пришедших невовремя.
  *              Они должны быть отработаны одним из следующих состояний.
  * @param nodesRespTs timestamp последнего обработанного запроса поиска узлов.
  * @param followCurrLoc Следовать ли карте за текущей геолокацией юзера?
  */
case class MbFsmSd(
  mapInst       : Option[MMapInst]    = None,
  lastUserLoc   : Option[MGeoLoc]     = None,
  early         : List[IFsmMsg]       = Nil,
  nodesRespTs   : Option[Long]        = None,
  followCurrLoc : Boolean             = false
)
