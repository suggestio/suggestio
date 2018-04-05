package io.suggest.adn.edit

import com.softwaremill.macwire._
import io.suggest.adn.edit.v.LkAdnEditFormR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:35
  * Description: macwire compile-time DI.
  */
class LkAdnEditModule {

  lazy val lkAdnEditCircuit = wire[LkAdnEditCircuit]


  // views

  lazy val lkAdnEditFormR = wire[LkAdnEditFormR]

}
