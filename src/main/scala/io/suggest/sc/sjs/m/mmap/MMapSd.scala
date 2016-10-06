package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.sjs.m.mgeo.MGeoLoc
import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.16 14:28
  * Description: Под-состояние Search FSM.
  * @param mapInst Инстанс мапы.
  * @param lastUserLoc Последняя полученная геолокация юзера.
  * @param followCurrLoc Следовать ли карте за текущей геолокацией юзера?
  * @param early Аккамулятор необработанных сообщений, пришедших невовремя.
  *              Они должны быть отработаны одним из следующих состояний.
  */
case class MMapSd(
  early         : List[IFsmMsg]       = Nil,
  mapInst       : Option[MMapInst]    = None,
  lastUserLoc   : Option[MGeoLoc]     = None,
  followCurrLoc : Boolean             = false,
  movingTimerId : Option[Int]         = None
)
