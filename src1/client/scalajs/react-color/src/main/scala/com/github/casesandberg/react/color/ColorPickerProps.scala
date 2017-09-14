package com.github.casesandberg.react.color

import japgolly.scalajs.react.ReactEvent

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 10:11
  * Description: Common props for all color pickers.
  */
trait ColorPickerProps extends js.Object {

  val color: Color_t

  val onChange: UndefOr[js.Function2[Color, ReactEvent, _]] = js.undefined

  val onChangeComplete: UndefOr[js.Function2[Color, ReactEvent, _]] = js.undefined

}


trait DisableAlpha extends ColorPickerProps {
  val disableAlpha: UndefOr[Boolean] = js.undefined
}


trait Width extends ColorPickerProps {
  val width: UndefOr[Int] = js.undefined
}


trait Renderers extends ColorPickerProps {
  val renderers: js.UndefOr[js.Object] = js.undefined
}


trait OnSwatchHover extends ColorPickerProps {
  val onSwatchHover: js.UndefOr[js.Function2[Color, ReactEvent, _]] = js.undefined
}
