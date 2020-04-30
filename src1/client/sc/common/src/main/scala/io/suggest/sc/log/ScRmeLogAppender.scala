package io.suggest.sc.log

import io.suggest.routes.ScJsRoutes
import io.suggest.log.Severities
import io.suggest.log.remote.RmeLogAppender

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 11:11
  * Description: Логгер, отправляющий ошибки на сервер.
  */
class ScRmeLogAppender extends RmeLogAppender {

  override def route        = ScJsRoutes.controllers.Sc.handleScError()

  override def minSeverity  = Severities.Error

}
