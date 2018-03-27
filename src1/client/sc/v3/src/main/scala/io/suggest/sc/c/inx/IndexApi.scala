package io.suggest.sc.c.inx

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.MSc3Resp
import io.suggest.sc.u.ScJsRoutesUtil

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
      route = ScJsRoutes.controllers.Sc.index
    )
  }

}
