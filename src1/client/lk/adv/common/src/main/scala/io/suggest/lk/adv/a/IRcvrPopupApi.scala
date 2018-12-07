package io.suggest.lk.adv.a

import io.suggest.adv.rcvr.MRcvrPopupResp
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, HttpRespTypes, Xhr}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.17 22:11
  * Description: Серверное API для попапа ресиверов.
  */
trait IRcvrPopupApi {

  /** Запрос с сервера попапа над ресивером. */
  def rcvrPopup(nodeId: String): Future[MRcvrPopupResp]

}


trait RcvrPopupHttpApiImpl extends IRcvrPopupApi {

  protected def _rcvrPopupRoute(nodeId: String): Route


  /** Запрос с сервера попапа над ресивером. */
  override def rcvrPopup(nodeId: String): Future[MRcvrPopupResp] = {
    val req = HttpReq.routed(
      route = _rcvrPopupRoute(nodeId),
      data  = HttpReqData(
        headers  = HttpReqData.headersBinaryAccept,
        respType = HttpRespTypes.ArrayBuffer
      )
    )
    Xhr.execute( req )
      .successIf200
      .unBooPickle[MRcvrPopupResp]
  }

}
