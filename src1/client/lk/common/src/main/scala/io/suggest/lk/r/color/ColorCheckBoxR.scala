package io.suggest.lk.r.color

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.m.ColorCheckboxChange
import io.suggest.lk.r.LkCss
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import io.suggest.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 11:38
  * Description: React-компонент с галочкой выбора цвета.
  */
class ColorCheckBoxR(
                      lkCss           : LkCss,
                      val colorBtnR   : ColorBtnR,
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
  implicit object ColorCheckBoxPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
      (a.label ===* b.label) &&
      (a.marker ===* b.marker)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  private val _colorBtnCssOpt = Option( lkCss.ColorOptPicker.colorRound.htmlClass )

  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клики по галочке заливки цветом. */
    private def _onCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        ColorCheckboxChange(isChecked, marker = props.value.flatMap(_.marker))
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      // Кнопка активации color-picker'а:
      propsOptProxy.value.whenDefinedEl { props =>
        val C = lkCss.ColorOptPicker

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

          props.color.whenDefined { mcd =>
            <.span(
              ^.`class` := Css.flat(Css.CLICKABLE, Css.Display.INLINE_BLOCK),
              ^.onClick ==> ReactCommonUtil.stopPropagationCB,

              HtmlConstants.NBSP_STR,
              HtmlConstants.NBSP_STR,

              // Кнопка активации color-picker'а:
              propsOptProxy.wrap { _ =>
                val p = colorBtnR.PropsVal(
                  color     = mcd,
                  cssClass  = _colorBtnCssOpt,
                  marker    = props.marker,
                )
                Some(p): colorBtnR.Props_t
              }( colorBtnR.apply ),

            )
          }

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def _apply(propsValOptProxy: Props) = component( propsValOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}