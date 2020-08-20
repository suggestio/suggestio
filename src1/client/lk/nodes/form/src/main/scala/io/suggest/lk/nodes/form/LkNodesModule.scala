package io.suggest.lk.nodes.form

import com.softwaremill.macwire._
import io.suggest.lk.nodes.form.r.{LkNodesFormCss, LkNodesFormR}
import io.suggest.lk.nodes.form.r.pop._
import io.suggest.lk.nodes.form.r.tree._
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import japgolly.scalajs.react.React

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.18 11:48
  * Description: compile-time DI линковка react LkNodes-формы.
  */
trait LkNodesModuleBase {

  import io.suggest.ReactCommonModule._

  lazy val lkNodesFormCircuit = wire[LkNodesFormCircuit]

  // views
  lazy val lkNodesFormR = wire[LkNodesFormR]

  // css
  lazy val lkNodesFormCssP: React.Context[LkNodesFormCss] =
    React.Context( wire[LkNodesFormCss] )

  // tree
  lazy val treeR = wire[TreeR]
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

  // pop
  lazy val createNodeR = wire[CreateNodeR]
  lazy val editTfDailyR = wire[EditTfDailyR]
  lazy val lknPopupsR = wire[LknPopupsR]
  lazy val nameEditDiaR = wire[NameEditDiaR]

  def getPlatformCss: () => PlatformCssStatic
  def platformComponents: PlatformComponents

}


/** Дефолтовая линковка для изолированной формы LkNodes на отдельной страницы внутри ЛК. */
final class LkNodesModule extends LkNodesModuleBase {

  /** Не особо запариваемся с тонкостями оформления. */
  lazy val platformCssStatic = PlatformCssStatic(
    isRenderIos = false,
  )

  override def getPlatformCss = () => platformCssStatic

  override lazy val platformComponents = wire[PlatformComponents]

}
