package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.dev.OsFamiliesR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.2020 2:06
  */
object ScCommonModule {

  lazy val osFamiliesR = wire[OsFamiliesR]

}
