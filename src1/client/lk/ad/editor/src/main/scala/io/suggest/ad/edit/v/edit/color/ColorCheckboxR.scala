package io.suggest.ad.edit.v.edit.color

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.m.ColorCheckboxChange
import io.suggest.lk.r.color.ColorBtnR
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 11:38
  * Description: React-компонент с галочкой выбора цвета.
  */
class ColorCheckboxR(
                      lkAdEditCss     : LkAdEditCss,
                      val colorBtnR   : ColorBtnR
                    ) {

  import colorBtnR.ColorBtnRPropsValFastEq

  /** Модель пропертисов этого компонента.
    *
    * @param color Цвет.
    *              None значит прозрачный.
    */
  case class PropsVal(
                       color          : Option[MColorData],
                       label          : String,
                       marker         : Option[IColorPickerMarker],
                     )
  implicit object ColorCheckboxPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
      (a.label ===* b.label) &&
      (a.marker ===* b.marker)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  case class State(
                    colorBtnPropsC    : ReactConnectProxy[colorBtnR.Props_t]
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Реакция на клики по галочке заливки цветом. */
    private def _onCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        ColorCheckboxChange(isChecked, marker = props.value.flatMap(_.marker))
      }
    }

    def render(propsOptProxy: Props, s: State): VdomElement = {
      // Кнопка активации color-picker'а:
      val innerBtnC = s.colorBtnPropsC { colorBtnR.apply }

      propsOptProxy.value.whenDefinedEl { props =>
        val C = lkAdEditCss.BgColorOptPicker

        <.div(
          C.container,

          // Галочка для переключения между прозрачным фоном и заливкой цветом
          <.label(
            C.label,

            <.input(
              ^.`type` := HtmlConstants.Input.checkbox,
              ^.checked := props.color.nonEmpty,
              ^.onChange ==> _onCheckBoxChanged
            ),

            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),
            <.span(
              ^.`class` := Css.flat(Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR),
              props.label
            )
          ),

          props.color.whenDefined { _ =>
            <.span(
              ^.`class` := Css.flat(Css.CLICKABLE, Css.Display.INLINE_BLOCK),
              ^.onClick ==> ReactCommonUtil.stopPropagationCB,

              HtmlConstants.NBSP_STR,
              HtmlConstants.NBSP_STR,

              // Кнопка активации color-picker'а:
              innerBtnC,

            )
          }

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .initialStateFromProps { propsOptProxy =>
      val colorBtnCssOpt = Option( lkAdEditCss.BgColorOptPicker.colorRound.htmlClass )
      State(
        colorBtnPropsC = propsOptProxy.connect { propsOpt =>
          for {
            props <- propsOpt
            color <- props.color
          } yield {
            colorBtnR.PropsVal(
              color     = color,
              cssClass  = colorBtnCssOpt,
              marker    = props.marker,
            )
          }
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build

  def _apply(propsValOptProxy: Props) = component( propsValOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
