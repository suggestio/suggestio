package io.suggest.ads

import com.softwaremill.macwire._
import io.suggest.ads.v.{LkAdsFormR, AdItemR}
import io.suggest.jd.render.JdRenderModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:18
  * Description: DI-модуль.
  */
class LkAdsModule {

  val jdRenderModule = wire[JdRenderModule]
  import jdRenderModule._

  lazy val lkAdsFormR = wire[LkAdsFormR]

  lazy val adItemR = wire[AdItemR]

  lazy val lkAdsCircuit = wire[LkAdsCircuit]

}
