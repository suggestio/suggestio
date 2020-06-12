package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.06.2020 14:41
  * Description: grid (flex-box) API
  * @see [[https://material-ui.com/api/grid/]]
  * @see [[https://material-ui.com/components/grid/]]
  */
object MuiGrid {

  val component = JsComponent[MuiGridProps, Children.Varargs, Null]( Mui.Grid )

  def apply(props: MuiGridProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  private object W {
    def CENTER = "center"
    def FLEX_START = "flex-start"
    def FLEX_END = "flex-end"
    def STRETCH = "stretch"
  }


  type AlignContent <: js.Any
  object AlignContent {
    def stretch = W.STRETCH.asInstanceOf[AlignContent]
    def center = W.CENTER.asInstanceOf[AlignContent]
    def `flex-start` = W.FLEX_START.asInstanceOf[AlignContent]
    def `flex-end` = W.FLEX_END.asInstanceOf[AlignContent]
    def `space-between` = "space-between".asInstanceOf[AlignContent]
    def `space-around` = "space-around".asInstanceOf[AlignContent]
  }


  type AlignItems <: js.Any
  object AlignItems {
    def `flex-start` = W.FLEX_START.asInstanceOf[AlignItems]
    def center = W.CENTER.asInstanceOf[AlignItems]
    def `flex-end` = W.FLEX_END.asInstanceOf[AlignItems]
    def stretch = W.STRETCH.asInstanceOf[AlignItems]
    def baseline = "baseline".asInstanceOf[AlignItems]
  }


  type Direction <: js.Any
  object Direction {
    def row = "row".asInstanceOf[Direction]
    def `row-reverse` = "row-reverse".asInstanceOf[Direction]
    def column = "column".asInstanceOf[Direction]
    def `column-reverse` = "column-reverse".asInstanceOf[Direction]
  }


  type Justify <: js.Any
  object Justify {
    def `flex-start` = W.FLEX_START.asInstanceOf[Justify]
    def center = W.CENTER.asInstanceOf[Justify]
    def `flex-end` = W.FLEX_END.asInstanceOf[Justify]
    def `space-between` = "space-between".asInstanceOf[Justify]
    def `space-around` = "space-around".asInstanceOf[Justify]
    def `space-evenly` = "space-evenly".asInstanceOf[Justify]
  }


  type Size = Boolean | String | Int
  object Size {
    def auto = "auto".asInstanceOf[Size]
  }


  type Wrap <: js.Any
  object Wrap {
    def nowrap = "nowrap".asInstanceOf[Wrap]
    def wrap = "wrap".asInstanceOf[Wrap]
    def `wrap-reverse` = "wrap-reverse".asInstanceOf[Wrap]
  }

}


trait MuiGridProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiGridClasses]
  with MuiPropsBaseComponent
{
  val alignContent: js.UndefOr[MuiGrid.AlignContent] = js.undefined
  val alignItems: js.UndefOr[MuiGrid.AlignItems] = js.undefined
  val container: js.UndefOr[Boolean] = js.undefined
  val direction: js.UndefOr[MuiGrid.Direction] = js.undefined
  val item: js.UndefOr[Boolean] = js.undefined
  val justify: js.UndefOr[MuiGrid.Justify] = js.undefined
  val lg: js.UndefOr[MuiGrid.Size] = js.undefined
  val md: js.UndefOr[MuiGrid.Size] = js.undefined
  val sm: js.UndefOr[MuiGrid.Size] = js.undefined
  val spacing: js.UndefOr[Int] = js.undefined
  val wrap: js.UndefOr[MuiGrid.Wrap] = js.undefined
  val xl: js.UndefOr[MuiGrid.Size] = js.undefined
  val xs: js.UndefOr[MuiGrid.Size] = js.undefined
  val zeroMinWidth: js.UndefOr[Boolean] = js.undefined
}


trait MuiGridClasses extends MuiClassesBase {
  val container: js.UndefOr[String] = js.undefined
  val item: js.UndefOr[String] = js.undefined
  val zeroMinWidth: js.UndefOr[String] = js.undefined

  val `direction-xs-column`: js.UndefOr[String] = js.undefined
  val `direction-xs-column-reverse`: js.UndefOr[String] = js.undefined
  val `direction-xs-row-reverse`: js.UndefOr[String] = js.undefined

  val `wrap-xs-nowrap`: js.UndefOr[String] = js.undefined
  val `wrap-xs-wrap-reverse`: js.UndefOr[String] = js.undefined

  val `align-items-xs-center`: js.UndefOr[String] = js.undefined
  val `align-items-xs-flex-start`: js.UndefOr[String] = js.undefined
  val `align-items-xs-flex-end`: js.UndefOr[String] = js.undefined
  val `align-items-xs-baseline`: js.UndefOr[String] = js.undefined

  val `align-content-xs-center`: js.UndefOr[String] = js.undefined
  val `align-content-xs-flex-start`: js.UndefOr[String] = js.undefined
  val `align-content-xs-flex-end`: js.UndefOr[String] = js.undefined
  val `align-content-xs-space-between`: js.UndefOr[String] = js.undefined
  val `align-content-xs-space-around`: js.UndefOr[String] = js.undefined

  val `justify-xs-center`: js.UndefOr[String] = js.undefined
  val `justify-xs-flex-end`: js.UndefOr[String] = js.undefined
  val `justify-xs-space-between`: js.UndefOr[String] = js.undefined
  val `justify-xs-space-around`: js.UndefOr[String] = js.undefined
  val `justify-xs-space-evenly`: js.UndefOr[String] = js.undefined

  val `spacing-xs-1`: js.UndefOr[String] = js.undefined
  val `spacing-xs-2`: js.UndefOr[String] = js.undefined
  val `spacing-xs-4`: js.UndefOr[String] = js.undefined
  val `spacing-xs-5`: js.UndefOr[String] = js.undefined
  val `spacing-xs-6`: js.UndefOr[String] = js.undefined
  val `spacing-xs-7`: js.UndefOr[String] = js.undefined
  val `spacing-xs-8`: js.UndefOr[String] = js.undefined
  val `spacing-xs-9`: js.UndefOr[String] = js.undefined
  val `spacing-xs-10`: js.UndefOr[String] = js.undefined

  val `grid-xs-auto`: js.UndefOr[String] = js.undefined
  val `grid-xs-true`: js.UndefOr[String] = js.undefined

  val `grid-xs-1`: js.UndefOr[String] = js.undefined
  val `grid-xs-2`: js.UndefOr[String] = js.undefined
  val `grid-xs-3`: js.UndefOr[String] = js.undefined
  val `grid-xs-4`: js.UndefOr[String] = js.undefined
  val `grid-xs-5`: js.UndefOr[String] = js.undefined
  val `grid-xs-6`: js.UndefOr[String] = js.undefined
  val `grid-xs-7`: js.UndefOr[String] = js.undefined
  val `grid-xs-8`: js.UndefOr[String] = js.undefined
  val `grid-xs-9`: js.UndefOr[String] = js.undefined
  val `grid-xs-10`: js.UndefOr[String] = js.undefined
  val `grid-xs-11`: js.UndefOr[String] = js.undefined
  val `grid-xs-12`: js.UndefOr[String] = js.undefined
}
