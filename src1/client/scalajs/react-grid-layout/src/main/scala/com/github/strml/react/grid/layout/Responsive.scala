package com.github.strml.react.grid.layout

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 17:11
  * Description: API for RGL Responsive.
  */
object Responsive {

  val component = JsComponent[ResponsiveProps, Children.Varargs, js.Object](ResponsiveJs)

  def apply(rglProps: ResponsiveProps)(children: VdomNode*) = component( rglProps )(children: _*)

}


@js.native
@JSImport("react-grid-layout", "Responsive")
object ResponsiveJs extends js.Object


/** Props. */
trait ResponsiveProps extends RglPropsCommon {

  /** {name: pxVal}, e.g. {lg: 1200, md: 996, sm: 768, xs: 480}
    * Breakpoint names are arbitrary but must match in the cols and layouts objects.
    */
  val breakpoints             : js.UndefOr[js.Dictionary[Int]]              = js.undefined

  /** # of cols. This is a breakpoint -> cols map, e.g. {lg: 12, md: 10, ...}. */
  val cols                    : js.UndefOr[js.Dictionary[Int]]              = js.undefined

  /** layouts is an object mapping breakpoints to layouts.
    * e.g. {lg: Layout, md: Layout, ...}
    */
  val layouts                 : js.UndefOr[js.Dictionary[js.Array[Layout]]] = js.undefined

  /** Calls back with breakpoint and new # cols
    * (newBreakpoint: string, newCols: number) => void
    */
  val onBreakpointChange      : js.UndefOr[js.Function2[String, Int, _]]    = js.undefined

  /** Callback so you can save the layout.
    * AllLayouts are keyed by breakpoint.
    * (currentLayout: Layout, allLayouts: {[key: $Keys<breakpoints>]: Layout}) => void
    */
  val onLayoutChange: js.UndefOr[js.Function2[Layout, js.Dictionary[Layout], _]] = js.undefined

  /** Callback when the width changes, so you can modify the layout as needed.
    * (containerWidth: number, margin: [number, number], cols: number, containerPadding: [number, number]) => void
    */
  val onWidthChange: js.UndefOr[js.Function4[Int, js.Array[Int], Int, js.Array[Int], _]] = js.undefined

}
