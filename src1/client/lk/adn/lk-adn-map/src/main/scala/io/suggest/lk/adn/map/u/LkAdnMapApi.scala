package io.suggest.lk.adn.map.u

import io.suggest.adn.mapf.MLamForm
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.router.jsRoutes
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 16:22
  * Description: Серверная APIшка до контроллера LkAdnMap.
  */
trait ILkAdnMapApi {

  /** Получение ценника. */
  def getPriceSubmit(nodeId: String, mForm: MLamForm): Future[MGetPriceResp]

  /** Сабмит формы на сервер. */
  def forNodeSubmit(nodeId: String, mForm: MLamForm): Future[String]

}


/** Реализация [[ILkAdnMapApi]] поверх обычного HTTP. */
class LkAdnMapApiHttpImpl extends ILkAdnMapApi {

  import LkAdnMapControllers._


  override def getPriceSubmit(nodeId: String, mForm: MLamForm): Future[MGetPriceResp] = {
    Xhr.unBooPickleResp[MGetPriceResp](
      Xhr.requestBinary(
        route = jsRoutes.controllers.LkAdnMap.getPriceSubmit(nodeId),
        body  = PickleUtil.pickle( mForm )
      )
    )
  }


  override def forNodeSubmit(nodeId: String, mForm: MLamForm): Future[String] = {
    for {
      xhr <- Xhr.successIf200 {
        Xhr.sendBinary(
          route     = jsRoutes.controllers.LkAdnMap.forNodeSubmit(nodeId),
          body      = PickleUtil.pickle( mForm ),
          respType  = Xhr.RespTypes.ANY
        )
      }
    } yield {
      xhr.responseText
    }
  }

}
