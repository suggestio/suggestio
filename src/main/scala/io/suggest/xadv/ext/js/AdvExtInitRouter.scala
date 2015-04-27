package io.suggest.xadv.ext.js

import io.suggest.sjs.common.controller.{InitRouter, InitController}
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.xadv.ext.js.form.FormEventsRiCtl
import io.suggest.xadv.ext.js.runner.c.RunnerRiCtl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 17:44
 * Description: RoutedInit-плагин направленной инициализации js-подсистем adv-ext.
 */
trait AdvExtInitRouter extends InitRouter {

  /** Поиск ri-контроллера с указанным именем (ключом).
    * Реализующие трейты должны переопределять этот метод под себя, сохраняя super...() вызов. */
  override protected def getController(name: String): Option[InitController] = {
    if (name == "LkAdvExt") {
      Some(new AdvExtInitController)
    } else {
      super.getController(name)
    }
  }

}

/** Реализация RoutedInit-контроллера: кучкуем все экшены. */
class AdvExtInitController
  extends FormEventsRiCtl
  with RunnerRiCtl
  with SjsLogger
