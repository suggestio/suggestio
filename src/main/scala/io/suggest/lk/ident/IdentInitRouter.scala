package io.suggest.lk.ident

import io.suggest.lk.ident.center.CenterContentAction
import io.suggest.sjs.common.controller.{InitRouter, InitController}
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 22:32
 * Description: Init-контроллер и его роутер для поддержки инициализаии при серверном ident-контроллере.
 */
trait IdentInitRouter extends InitRouter {
  override protected def getController(name: String): Option[InitController] = {
    if (name == "Ident") {
      Some(new IdentInitController)
    } else {
      super.getController(name)
    }
  }
}

class IdentInitController extends CenterContentAction with SjsLogger
