package io.suggest.sc.inx.c

import io.suggest.routes.scRoutes
import io.suggest.sc.index.MScIndexArgs
import io.suggest.routes.JsRoutes_ScControllers._
import io.suggest.sc.router.c.ScJsRoutesUtil
import io.suggest.sc.sc3.MSc3Resp

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:54
  * Description: Интерфейс API для получения индекса с сервера.
  */
trait IIndexApi {

  /** Получить индекс с сервера, вернув MSc3Resp.
    *
    * @param args Аргументы запроса индекса.
    * @return Фьючерс с ответом сервера.
    */
  def getIndex(args: MScIndexArgs): Future[MSc3Resp]

}


/** Реализация [[IIndexApi]] поверх XHR. */
trait IndexApiXhrImpl extends IIndexApi {

  override def getIndex(args: MScIndexArgs): Future[MSc3Resp] = {
    ScJsRoutesUtil.mkSc3Request(
      args  = args,
      route = scRoutes.controllers.Sc.index
    )
  }

}
