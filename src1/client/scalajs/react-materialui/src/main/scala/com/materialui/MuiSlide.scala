package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.2021 18:25
  * @see [[https://material-ui.com/ru/api/slide/]]
  */
object MuiSlide {

  val component = JsComponent[MuiSlideProps, Children.Varargs, Null]( Mui.Slide )

  def apply( props: MuiSlideProps = MuiPropsBaseStatic.empty )( children: VdomElement ) =
    component( props )( children )

}


trait MuiSlideProps
  extends MuiPropsBase // TODO extends ReactTransitionGroup/TransitionProps
{
  val direction: js.UndefOr[MuiSlideDirection] = js.undefined
  val in: js.UndefOr[Boolean] = js.undefined
  val timeout: js.UndefOr[Double | MuiTransitionDuration] = js.undefined
}


object MuiSlideDirection {
  final def DOWN    = "down".asInstanceOf[MuiSlideDirection]
  final def LEFT    = "left".asInstanceOf[MuiSlideDirection]
  final def RIGHT   = "right".asInstanceOf[MuiSlideDirection]
  final def UP      = "up".asInstanceOf[MuiSlideDirection]
}
