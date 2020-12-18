package io.suggest.lk.adn.map

import com.softwaremill.macwire._
import io.suggest.lk.adn.map.r._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2020 18:48
  */
final class LkAdnMapFormModule {

  import io.suggest.lk.LkCommonModule._

  lazy val lkAdnMapCircuit = wire[LkAdnMapCircuit]

  lazy val lamFormR = wire[LamFormR]

  lazy val lamPopupsR = wire[LamPopupsR]

  lazy val currentGeoR = wire[CurrentGeoR]

  lazy val lamRcvrsR = wire[LamRcvrsR]

  lazy val mapCursorR = wire[MapCursorR]

  lazy val radPopupR = wire[RadPopupR]

  lazy val optsR = wire[OptsR]

}
