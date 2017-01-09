package io.suggest.sc.sjs.m.mwc

import io.suggest.sjs.common.model.TimeoutPromise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.16 16:00
  * Description: Класс, описывающий состояние текущего welcome'а, если он существует в DOM.
  */

case class WcHideState(
  isHiding: Boolean,
  info: TimeoutPromise
)
