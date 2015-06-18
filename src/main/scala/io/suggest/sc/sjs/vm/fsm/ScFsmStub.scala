package io.suggest.sc.sjs.vm.fsm

import io.suggest.fsm.{AbstractFsm, AbstractFsmUtil}
import io.suggest.sjs.common.util.ISjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends AbstractFsm with ISjsLogger {

  override type Receive = PartialFunction[IFsmMsg, Unit]

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }

}
