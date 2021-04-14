package io.suggest.ads

import com.softwaremill.macwire._
import io.suggest.ads.v.{AdItemR, LkAdsFormR}
import io.suggest.lk.api.LkAdsApiHttp
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.proto.http.model.IMHttpClientConfig

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:18
  * Description: DI-модуль.
  */
class LkAdsModule extends IMHttpClientConfig {

  import io.suggest.jd.render.JdRenderModule._

  lazy val lkAdsFormR = wire[LkAdsFormR]

  lazy val adItemR = wire[AdItemR]

  lazy val lkAdsApi = wire[LkAdsApiHttp]

  def nodesDiConf = NodesDiConf.LkConf
  lazy val lkNodesApi = wire[LkNodesApiHttpImpl]

  lazy val lkAdsCircuit = wire[LkAdsCircuit]

}
