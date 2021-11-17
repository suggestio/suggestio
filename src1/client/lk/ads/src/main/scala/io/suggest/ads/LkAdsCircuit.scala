package io.suggest.ads

import diode.react.ReactConnector
import io.suggest.ads.c.NodeAdsAh
import io.suggest.ads.m.{GetMoreAds, MAdsS, MLkAdsRoot}
import io.suggest.dev.MSzMults
import io.suggest.jd.MJdConf
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.u.JdUtil
import io.suggest.lk.api.ILkAdsApi
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.log.CircuitLog
import io.suggest.spa.{FastEqUtil, StateInp}
import play.api.libs.json.Json
import io.suggest.spa.CircuitUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:18
  * Description: Circuit react-формы управления карточками.
  */
class LkAdsCircuit(
                    lkAdsApi        : ILkAdsApi,
                    lkNodesApi      : ILkNodesApi,
                  )
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
        jdRuntime = JdUtil.prepareJdRuntime(jdConf).make,
      )
    )
  }

  // Модели
  val adsRW = mkLensRootZoomRW(this, MLkAdsRoot.ads)(MAdsS.MAdsSFastEq)
  val confRO = mkLensRootZoomRO(this, MLkAdsRoot.conf)( FastEqUtil.AnyRefFastEq )
  val jdRuntimeRW = mkLensZoomRW( adsRW, MAdsS.jdRuntime )


  val nodeAdsAh = new NodeAdsAh(
    api           = lkAdsApi,
    lkNodesApi    = lkNodesApi,
    confRO        = confRO,
    modelRW       = adsRW
  )

  val jdAh = new JdAh(
    modelRW = jdRuntimeRW,
  )


  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      nodeAdsAh,
      jdAh,
    )
  }


  // Запустить инициализацию списка карточек.
  dispatch( GetMoreAds(clean = true) )

}
