package io.suggest.lk.adv.direct.init

import io.suggest.lk.adv.direct.fsm.AdvDirectFormFsm
import io.suggest.lk.adv.direct.vm.Form
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 23:35
 * Description: Инициализация формы прямого размещения.
 */

trait AdvDirectFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdvDirectForm) {
      Future {
        (new AdvDirectFormInit).init()
      }
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
