package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import scalacss.ScalaCssReact._
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.18 22:00
  * Description: input-слайдер в личном кабинете.
  */
class InputSliderR(
                    lkCss: LkCss
                  ) {

  case class PropsVal(
                       min      : Int,
                       max      : Int,
                       value    : Int,
                       onChange : Int => DAction,
                     )
  implicit object InputSliderRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.min ==* b.min) &&
      (a.max ==* b.max) &&
      (a.value ==* b.value) &&
      (a.onChange eq b.onChange)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на движение слайдера градусов наклона. */
    private def onValueChange(e: ReactEventFromInput): Callback = {
      val v = e.target.value
      if (v.isEmpty) {
        Callback.empty
      } else {
        val newValue = v.toInt
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
          props.value.get.onChange( newValue )
        }
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        val C = lkCss.RangeInput
        <.span(
          <.input(
            C.slider,
            ^.`type`  := HtmlConstants.Input.range,
            ^.value   := props.value,
            ^.min     := props.min,
            ^.max     := props.max,
            ^.onChange ==> onValueChange
          ),

          HtmlConstants.NBSP_STR,

          <.span(
            ^.`class` := Css.Input.INPUT,
            <.input(
              C.textInput,
              ^.`type`    := HtmlConstants.Input.text,
              ^.value     := props.value,
              ^.onChange  ==> onValueChange
            )
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
