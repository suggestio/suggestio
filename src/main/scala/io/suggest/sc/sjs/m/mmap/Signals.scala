package io.suggest.sc.sjs.m.mmap

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:20
  * Description: Сообщения между ScFsm и MbFsm.
  */

/** Сигнал инициализации карты. */
case class EnsureMap() extends IFsmMsg

/** Сигнал о начале отображения карты на экране. */
case object MapShowing extends IFsmMsg

/** Сигнал для MbFsm о необходимости подготовления к смене index'а выдачи. */
case object ScInxWillSwitch extends IFsmMsg
