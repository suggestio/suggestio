package io.suggest.ads

import diode.react.ReactConnector
import io.suggest.ads.a.LkAdsApiHttp
import io.suggest.ads.c.NodeAdsAh
import io.suggest.ads.m.{GetMoreAds, MAdsS, MLkAdsRoot}
import io.suggest.dev.MSzMults
import io.suggest.jd.MJdConf
import io.suggest.jd.render.u.JdUtil
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.{FastEqUtil, StateInp}
import play.api.libs.json.Json
import io.suggest.spa.CircuitUtil._

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

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.LK_ADS_FORM_FAILED

  override protected def initialModel: MLkAdsRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MLkAdsFormInit]

    val jdConf = MJdConf(
      isEdit            = false,
      szMult            = MSzMults.`0.5`,
      gridColumnsCount  = 2
    )

    MLkAdsRoot(
      conf = MLkAdsConf(
        nodeKey = minit.nodeKey,
        jdConf  = jdConf
      ),
      ads = MAdsS(
        jdRuntime = JdUtil.mkRuntime(jdConf).make,
      )
    )
  }

  // Модели
  val currNodeRW = mkLensRootZoomRW(this, MLkAdsRoot.ads)(MAdsS.MAdsSFastEq)
  val confRO = mkLensRootZoomRO(this, MLkAdsRoot.conf)( FastEqUtil.AnyRefFastEq )


  // Контроллеры
  val lkAdsApi = new LkAdsApiHttp()

  val lkNodesApi = new LkNodesApiHttpImpl

  val nodeAdsAh = new NodeAdsAh(
    api           = lkAdsApi,
    lkNodesApi    = lkNodesApi,
    confRO        = confRO,
    modelRW       = currNodeRW
  )


  override protected val actionHandler: HandlerFunction = {
    nodeAdsAh
  }


  // Запустить инициализацию списка карточек.
  dispatch( GetMoreAds(clean = true) )

}
