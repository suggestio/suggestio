package io.suggest.sc.sjs.c.search

import io.suggest.primo.{IApply0, IStart0}
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 16:55
  * Description: Интерфейсы для сборки tabs FSM.
  */
trait ITabFsmFactory extends IApply0 {

  override type T <: SjsFsm with IStart0

}
