package com.materialui

import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import japgolly.scalajs.react.{Children, JsComponent, ReactEventFromHtml}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 21:30
  * Description: Tabs component API.
  * @see [[https://material-ui.com/api/tabs/]]
  */
object MuiTabs {

  val component = JsComponent[MuiTabsProps, Children.Varargs, Null]( Mui.Tabs )

  def apply(props: MuiTabsProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON Props for [[MuiTabs]] component. */
trait MuiTabsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTabsClasses]
  with MuiPropsBaseComponent
{
  /**
    * Callback fired when the component mounts. This is useful when you want to trigger an action programmatically. It currently only supports updateIndicator() action.
    * Signature: function(actions: object) => void
    * actions: This object contains all possible actions that can be triggered programmatically.
    */
  val action: js.UndefOr[js.Function1[MuiTabsActions, Unit]] = js.undefined
  val allowScrollButtonsMobile,
      centered
      : js.UndefOr[Boolean] = js.undefined
  /** 'secondary' | 'primary' */
  val indicatorColor: js.UndefOr[String] = js.undefined
  @JSName("onChange")
  val onTabChanged: js.UndefOr[js.Function2[ReactEventFromHtml, js.Any, Unit]] = js.undefined
  val ScrollButtonComponent: js.UndefOr[Component_t] = js.undefined
  /** @see [[MuiTabsScrollButtonsModes]] */
  val scrollButtons: js.UndefOr[String] = js.undefined
  val TabIndicatorProps: js.UndefOr[js.Object] = js.undefined
  val textColor: js.UndefOr[String] = js.undefined
  /** The value of the currently selected Tab.
    * If you don't want any selected Tab, you can set this property to false.
    */
  val value: js.UndefOr[js.Any] = js.undefined
  /** @see [[MuiTabsVariants]]. */
  val variant: js.UndefOr[String] = js.undefined
}


trait MuiTabsClasses extends MuiClassesBase {
  val flexContainer: js.UndefOr[String] = js.undefined
  val centered: js.UndefOr[String] = js.undefined
  val scroller: js.UndefOr[String] = js.undefined
  val fixed: js.UndefOr[String] = js.undefined
  val scrollable: js.UndefOr[String] = js.undefined
  val scrollButtons: js.UndefOr[String] = js.undefined
  val scrollButtonsAuto: js.UndefOr[String] = js.undefined
  val indicator: js.UndefOr[String] = js.undefined
}


@js.native
trait MuiTabsActions extends js.Object {
  def updateIndicator(): Unit = js.native
}


object MuiTabsScrollButtonsModes {
  def auto = "auto"
  def on = "on"
  def off = "off"
}


object MuiTabsVariants {
  def standard    = "standard"
  def scrollable  = "scrollable"
  def fullWidth   = "fullWidth"
}
