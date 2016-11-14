package io.suggest.sc.sjs.util.logs

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.model.rme.MRmeClientT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.16 9:29
  * Description: Трейт логгирования, используемый в sc-sjs.
  *
  * Вынесен в отдельный трейт для унифицированного управления логгированием в выдаче.
  * Появился в ожиданиях скорого внедрения server-side логгирования через MRemoteError.
  */
object ScSjsLogger extends MRmeClientT {

  override def route: Route = routes.controllers.Sc.handleScError()

}
