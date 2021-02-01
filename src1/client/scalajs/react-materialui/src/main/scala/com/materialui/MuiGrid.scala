package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

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

  val component = JsForwardRefComponent[MuiGridProps, Children.Varargs, dom.html.Element]( Mui.Grid )

  def apply(props: MuiGridProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  private object W {
    final def CENTER = "center"
    final def FLEX_START = "flex-start"
    final def FLEX_END = "flex-end"
    final def STRETCH = "stretch"
  }


  type AlignContent <: js.Any
  object AlignContent {
    final def stretch = W.STRETCH.asInstanceOf[AlignContent]
    final def center = W.CENTER.asInstanceOf[AlignContent]
    final def `flex-start` = W.FLEX_START.asInstanceOf[AlignContent]
    final def `flex-end` = W.FLEX_END.asInstanceOf[AlignContent]
    final def `space-between` = "space-between".asInstanceOf[AlignContent]
    final def `space-around` = "space-around".asInstanceOf[AlignContent]
  }


  type AlignItems <: js.Any
  object AlignItems {
    final def `flex-start` = W.FLEX_START.asInstanceOf[AlignItems]
    final def center = W.CENTER.asInstanceOf[AlignItems]
    final def `flex-end` = W.FLEX_END.asInstanceOf[AlignItems]
    final def stretch = W.STRETCH.asInstanceOf[AlignItems]
    final def baseline = "baseline".asInstanceOf[AlignItems]
  }


  type Direction <: js.Any
  object Direction {
    final def row = "row".asInstanceOf[Direction]
    final def `row-reverse` = "row-reverse".asInstanceOf[Direction]
    final def column = "column".asInstanceOf[Direction]
    final def `column-reverse` = "column-reverse".asInstanceOf[Direction]
  }


  type JustifyContent <: js.Any
  object JustifyContent {
    final def `flex-start` = W.FLEX_START.asInstanceOf[JustifyContent]
    final def center = W.CENTER.asInstanceOf[JustifyContent]
    final def `flex-end` = W.FLEX_END.asInstanceOf[JustifyContent]
    final def `space-between` = "space-between".asInstanceOf[JustifyContent]
    final def `space-around` = "space-around".asInstanceOf[JustifyContent]
    final def `space-evenly` = "space-evenly".asInstanceOf[JustifyContent]
  }


  type Size = Boolean | String | Int
  object Size {
    final def auto = "auto".asInstanceOf[Size]
  }


  type Wrap <: js.Any
  object Wrap {
    final def nowrap = "nowrap".asInstanceOf[Wrap]
    final def wrap = "wrap".asInstanceOf[Wrap]
    final def `wrap-reverse` = "wrap-reverse".asInstanceOf[Wrap]
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
  val justifyContent: js.UndefOr[MuiGrid.JustifyContent] = js.undefined
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
