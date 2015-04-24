package io.suggest.lk.ident

import io.suggest.lk.ident.center.CenterContentAction
import io.suggest.sjs.common.controller.{RoutedInitController, RoutedInit}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 22:32
 * Description: Init-контроллер и его роутер для поддержки инициализаии при серверном ident-контроллере.
 */
trait IdentInitRouter extends RoutedInit {
  override protected def getController(name: String): Option[RoutedInitController] = {
    if (name == "Ident") {
      Some(new IdentController)
    } else {
      super.getController(name)
    }
  }
}

class IdentController extends CenterContentAction
