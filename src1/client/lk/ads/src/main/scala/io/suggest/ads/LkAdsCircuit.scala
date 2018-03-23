package io.suggest.ads

import diode.react.ReactConnector
import io.suggest.ads.a.LkAdsApiHttp
import io.suggest.ads.c.NodeAdsAh
import io.suggest.ads.m.{GetMoreAds, MCurrNodeS, MLkAdsRoot}
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.StateInp
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:18
  * Description: Circuit react-формы управления карточками.
  */
class LkAdsCircuit
  extends CircuitLog[MLkAdsRoot]
  with ReactConnector[MLkAdsRoot]
{

  import MCurrNodeS.MCurrNodeSFastEq

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.LK_ADS_FORM_FAILED

  override protected def initialModel: MLkAdsRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MLkAdsFormInit]

    MLkAdsRoot(
      currNode = MCurrNodeS(
        nodeKey = minit.form.nodeKey
      )
    )
  }

  // Модели
  val currNodeRW = zoomRW(_.currNode) { _.withCurrNode(_) }


  // Контроллеры
  val lkAdsApi = new LkAdsApiHttp()

  val nodeAdsAh = new NodeAdsAh(
    api     = lkAdsApi,
    modelRW = currNodeRW
  )


  override protected def actionHandler: HandlerFunction = {
    nodeAdsAh
  }


  // TODO Запустить инициализацию списка карточек.
  //dispatch( GetMoreAds(clean = true) )

}
