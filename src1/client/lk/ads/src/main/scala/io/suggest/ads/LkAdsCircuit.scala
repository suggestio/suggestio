package io.suggest.ads

import diode.react.ReactConnector
import io.suggest.ads.a.LkAdsApiHttp
import io.suggest.ads.c.NodeAdsAh
import io.suggest.ads.m.{GetMoreAds, MAdsS, MLkAdsRoot}
import io.suggest.dev.MSzMults
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
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
class LkAdsCircuit(
                    jdCssFactory: JdCssFactory
                  )
  extends CircuitLog[MLkAdsRoot]
  with ReactConnector[MLkAdsRoot]
{

  import MAdsS.MAdsSFastEq

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
        jdCss = jdCssFactory.mkJdCss(
          MJdCssArgs(conf = jdConf)
        )
      )
    )
  }

  // Модели
  val currNodeRW = zoomRW(_.ads) { _.withCurrNode(_) }
  val confRO = zoom(_.conf)


  // Контроллеры
  val lkAdsApi = new LkAdsApiHttp()

  val lkNodesApi = new LkNodesApiHttpImpl

  val nodeAdsAh = new NodeAdsAh(
    api           = lkAdsApi,
    lkNodesApi    = lkNodesApi,
    jdCssFactory  = jdCssFactory,
    confRO        = confRO,
    modelRW       = currNodeRW
  )


  override protected val actionHandler: HandlerFunction = {
    nodeAdsAh
  }


  // Запустить инициализацию списка карточек.
  dispatch( GetMoreAds(clean = true) )

}
