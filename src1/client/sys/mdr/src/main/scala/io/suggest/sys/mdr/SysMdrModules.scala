package io.suggest.sys.mdr

import com.softwaremill.macwire._
import io.suggest.sys.mdr.v._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:24
  * Description: DI-classes for sys.mdr.
  */
class SysMdrModules {

  lazy val nodeMdrR = wire[NodeMdrR]
  lazy val mdrRowR = wire[MdrRowR]
  lazy val sysMdrFormR = wire[SysMdrFormR]

  lazy val sysMdrCircuit = wire[SysMdrCircuit]

}
