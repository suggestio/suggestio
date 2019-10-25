package io.suggest.lk.r

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.{DAction, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 22:14
  * Description: Компонент галочки и слайдера ширины.
  * Построен на базе отрефакторенного RotateR.
  * Нет общего компонента, т.к. есть отличия в деталях и возможен рост расхождения в будущем.
  */
class SliderOptR(
                  inputSliderR     : InputSliderR,
                  lkCheckBoxR      : LkCheckBoxR,
                ) {

  case class PropsVal(
                       label    : VdomNode,
                       value    : Option[Int],
                       onChange : Option[Int] => DAction,
                       min      : Int,
                       max      : Int,
                       default  : Int,
                     )
  implicit object SliderOptRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.label eq b.label) &&
      (a.value ==* b.value) &&
      (a.onChange eq b.onChange) &&
      (a.min ==* b.min) &&
      (a.max ==* b.max) &&
      (a.default ==* b.default)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC     : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsOptProxy: Props, s: State): VdomElement = {
      // Для ускорения первичного рендера, тут - общий lazy val, который хранит компоненты и чекбокса, и слайдера.
      lazy val (checkBox, slider) = {
        // Галочка опционального значения:
        val c = propsOptProxy.wrap { propsOpt =>
          for (props <- propsOpt) yield {
            lkCheckBoxR.PropsVal(
              label     = props.label,
              checked   = props.value.nonEmpty,
              onChange  = props.onChange
                .compose[Boolean] { OptionUtil.maybe(_)(props.default) },
            )
          }
        }( lkCheckBoxR.apply )(implicitly, OptFastEq.Wrapped(lkCheckBoxR.LkCheckBoxMinimalFastEq) )

        // Слайдер выставления значения:
        val s = propsOptProxy.wrap { propsOpt =>
          for (props <- propsOpt; v <- props.value) yield {
            inputSliderR.PropsVal(
              min       = props.min,
              max       = props.max,
              value     = v,
              onChange  = props.onChange.compose(Some.apply),
            )
          }
        }( inputSliderR.apply )(implicitly, OptFastEq.Wrapped(inputSliderR.InputSliderValuesPropsValFastEq) )

        (c, s)
      }

      // Надо вообще рендерить или нет - решается тут:
      s.isVisibleSomeC { isVisibleSomeProxy =>
        ReactCommonUtil.maybeEl( isVisibleSomeProxy.value.value ) {
          <.div(
            checkBox,
            HtmlConstants.NBSP_STR,
            slider,
          )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isVisibleSomeC = propsProxy.connect { m =>
          OptionUtil.SomeBool( m.isDefined )
        }( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
