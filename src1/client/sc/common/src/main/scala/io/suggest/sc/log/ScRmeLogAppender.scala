package io.suggest.sc.log

import io.suggest.sc.router.routes
import io.suggest.sjs.common.log.Severities
import io.suggest.sjs.common.model.rme.RmeLogAppender

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 11:11
  * Description: Логгер, отправляющий ошибки на сервер.
  */
class ScRmeLogAppender extends RmeLogAppender {

  override def route        = routes.controllers.Sc.handleScError()

  override def minSeverity  = Severities.ALL

}
