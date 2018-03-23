package io.suggest.ads

import com.softwaremill.macwire._
import io.suggest.ads.v.LkAdsFormR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:18
  * Description: DI-модуль.
  */
class LkAdsModule {

  lazy val lkAdsFormR = wire[LkAdsFormR]


  lazy val lkAdsCircuit = wire[LkAdsCircuit]

}
