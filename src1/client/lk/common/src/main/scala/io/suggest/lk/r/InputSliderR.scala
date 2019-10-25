package io.suggest.lk.r

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import scalacss.ScalaCssReact._
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.spa.{DAction, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
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
  object InputSliderValuesPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.min ==* b.min) &&
      (a.max ==* b.max) &&
      (a.value ==* b.value)
    }
  }
  implicit object InputSliderRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      InputSliderValuesPropsValFastEq.eqv(a, b) &&
      (a.onChange eq b.onChange)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  case class State(
                    isVisibleSomeC      : ReactConnectProxy[Some[Boolean]],
                    propsValOptC        : ReactConnectProxy[Props_t],
                    valueOptC           : ReactConnectProxy[Option[Int]],
                  )

  class Backend($: BackendScope[Props, State]) {

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

    def render(propsOptProxy: Props, s: State): VdomElement = {
      // Генератор контента.
      val content = <.span(
        // Слайдер:
        s.propsValOptC { propsValOptProxy =>
          val propsOpt = propsValOptProxy.value
          <.input(
            lkCss.RangeInput.slider,
            ^.`type`    := HtmlConstants.Input.range,
            ^.value     := propsOpt.fold(0)(_.value),
            ^.min       := propsOpt.fold(0)(_.min),
            ^.max       := propsOpt.fold(0)(_.max),
            ^.onChange ==> onValueChange,
          )
        },

        HtmlConstants.NBSP_STR,

        <.span(
          ^.`class` := Css.Input.INPUT,

          // Текстовый инпут для ручного ввода значения:
          s.valueOptC { valueOptProxy =>
            val valueOpt = valueOptProxy.value
            <.input(
              lkCss.RangeInput.textInput,
              ^.`type`    := HtmlConstants.Input.text,
              ^.value     := valueOpt.fold("")(_.toString),
              ^.onChange ==> onValueChange
            )
          },

        )
      )

      s.isVisibleSomeC { isVisibleSomeProxy =>
        content(
          ^.classSet(
            Css.Display.INVISIBLE -> !isVisibleSomeProxy.value.value,
          ),
        )
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
        propsValOptC = propsProxy.connect(identity)( OptFastEq.Wrapped(InputSliderValuesPropsValFastEq) ),
        valueOptC = propsProxy.connect( _.map(_.value) )( OptFastEq.OptValueEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
