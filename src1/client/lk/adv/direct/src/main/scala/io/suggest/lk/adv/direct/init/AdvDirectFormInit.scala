package io.suggest.lk.adv.direct.init

import io.suggest.lk.adv.direct.fsm.AdvDirectFormFsm
import io.suggest.lk.adv.direct.vm.Form
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 23:35
 * Description: Инициализация формы прямого размещения.
 */

trait AdvDirectFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Unit = {
    if (itg ==* MInitTargets.AdvDirectForm) {
      (new AdvDirectFormInit).init()
    } else {
      super.routeInitTarget(itg)
    }
  }

}



class AdvDirectFormInit
  extends IInitDummy
{

  override def init(): Unit = {
    super.init()
    val fsm = AdvDirectFormFsm
    fsm.start()
    for (form <- Form.find()) {
      form.initLayout(fsm)
    }
  }

}
