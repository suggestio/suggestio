package io.suggest.sc.u.api

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.{MSc3Resp, MScQs}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.05.18 21:38
  * Description: Поддержка sc UniApi.
  */
trait IScUniApi {

  def pubApi(args: MScQs): Future[MSc3Resp]

}


/** Реализация поддержки Sc UniApi поверх HTTP. */
trait ScUniApiHttpImpl extends IScUniApi {

  override def pubApi(args: MScQs): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args  = args,
      route = ScJsRoutes.controllers.Sc.pubApi
    )
  }

}
