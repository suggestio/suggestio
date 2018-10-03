package io.suggest.sys.mdr

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sys.mdr.v._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:24
  * Description: DI-classes for sys.mdr.
  */
class SysMdrModules {

  val jdModules = wire[JdRenderModule]
  import jdModules._

  lazy val nodeRenderR = wire[NodeRenderR]
  lazy val nodeMdrR = wire[NodeMdrR]
  lazy val mdrRowR = wire[MdrRowR]
  lazy val sysMdrFormR = wire[SysMdrFormR]

  lazy val sysMdrCircuit = wire[SysMdrCircuit]

}
