package io.suggest.lk.adv.direct.init

import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

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
