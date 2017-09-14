package com.github.casesandberg.react.color

import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.09.17 22:45
  * Description: Scala.js API for react-color SketchPicker.
  */
object Sketch {

  val component = JsComponent[SketchProps, Children.None, Null]( SketchJs )

  def apply(props: SketchProps) = component( props )

}


@JSImport("react-color", "SketchPicker")
@js.native
protected object SketchJs extends js.Object


/** Props for [[Sketch]]. */
// @ScalaJSDefined
trait SketchProps
  extends ColorPickerProps
  with DisableAlpha
  with Width
  with Renderers
  with OnSwatchHover
{

  val presetColors: js.UndefOr[PresetColors_t] = js.undefined

}
