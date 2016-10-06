package io.suggest.sc.sjs.c.search.tags

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mtags.MTagsSd
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.util.ISjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 14:57
  * Description: Трейт для сборки кусков FSM тегов.
  */
trait TagsFsmStub extends SjsFsm with StateData with ISjsLogger {

  override type SD = MTagsSd
  override type State_t = FsmState

}
