package io.suggest.lk.nodes.form

import com.softwaremill.macwire._
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.lk.{IPlatformComponentsModule, PlatformComponentsModuleDflt}
import io.suggest.lk.nodes.form.r.{LkNodesFormCss, LkNodesFormR}
import io.suggest.lk.nodes.form.r.pop._
import io.suggest.lk.nodes.form.r.tree._
import io.suggest.lk.r.DeleteConfirmPopupR
import japgolly.scalajs.react.React

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.18 11:48
  * Description: compile-time DI линковка react LkNodes-формы.
  */
trait LkNodesModuleBase
  extends IPlatformComponentsModule
{

  import io.suggest.ReactCommonModule._

  def diConfig: NodesDiConf

  lazy val lkNodesApi = wire[LkNodesApiHttpImpl]
  def lkNodesFormCircuit = wire[LkNodesFormCircuit]

  // views
  lazy val lkNodesFormR = wire[LkNodesFormR]

  // css
  lazy val lkNodesFormCssP: React.Context[LkNodesFormCss] =
    React.Context( wire[LkNodesFormCss] )

  // tree
  lazy val treeR = wire[TreeR]
  lazy val treeItemR = wire[TreeItemR]
  lazy val goToLkLinkR = wire[GoToLkLinkR]
  lazy val nodeR = wire[NodeR]
  lazy val nodeEnabledR = wire[NodeEnabledR]
  lazy val nameEditButtonR = wire[NameEditButtonR]
  lazy val nodeScLinkR = wire[NodeScLinkR]
  lazy val boolPotCheckBox = wire[NodeHeaderR]
  lazy val deleteBtnR = wire[DeleteBtnR]
  lazy val tariffEditR = wire[TariffEditR]
  lazy val subNodesR = wire[SubNodesR]
  lazy val nodeAdvRowR = wire[NodeAdvRowR]
  lazy val nodeToolBarR = wire[NodeToolBarR]
  lazy val distanceR = wire[DistanceRowR]
  lazy val distanceValueR = wire[DistanceValueR]
  lazy val beaconInfoR = wire[BeaconInfoR]
  lazy val treeStuffR = wire[TreeStuffR]
  lazy val nfcBtnR = wire[NfcBtnR]

  // pop
  lazy val deleteConfirmPopupR = wire[DeleteConfirmPopupR]
  lazy val createNodeR = wire[CreateNodeR]
  lazy val editTfDailyR = wire[EditTfDailyR]
  lazy val lknPopupsR = wire[LknPopupsR]
  lazy val nameEditDiaR = wire[NameEditDiaR]
  lazy val nfcDialogR = wire[NfcDialogR]

}


/** Дефолтовая линковка для изолированной формы LkNodes на отдельной страницы внутри ЛК. */
final class LkNodesModule
  extends LkNodesModuleBase
  with PlatformComponentsModuleDflt
{

  override def diConfig = NodesDiConf.LkConf

  override lazy val lkNodesFormCircuit = super.lkNodesFormCircuit

}
