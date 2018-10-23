package io.suggest.sys.mdr

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sys.mdr.v._
import io.suggest.sys.mdr.v.dia.MdrDiaRefuseR
import io.suggest.sys.mdr.v.main.{MdrErrorsR, NodeRenderR}
import io.suggest.sys.mdr.v.pane._
import io.suggest.sys.mdr.v.toolbar._

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
  lazy val mdrSidePanelR = wire[MdrSidePanelR]
  lazy val mdrRowR = wire[MdrRowR]
  lazy val sysMdrFormR = wire[SysMdrFormR]
  lazy val mdrErrorsR = wire[MdrErrorsR]
  lazy val mdrDiaRefuseR = wire[MdrDiaRefuseR]

  lazy val mdrToolBarR = wire[MdrToolBarR]
  lazy val mdrTbAnchorBtnR = wire[MdrTbAnchorBtnR]
  lazy val mdrTbStepBtnR = wire[MdrTbStepBtnR]

  lazy val sysMdrCircuit = wire[SysMdrCircuit]

}
