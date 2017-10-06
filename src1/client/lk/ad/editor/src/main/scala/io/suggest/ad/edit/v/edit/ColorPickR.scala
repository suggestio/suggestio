package io.suggest.ad.edit.v.edit

import com.github.casesandberg.react.color._
import diode.react.ModelProxy
import io.suggest.ad.edit.m.{ColorBtnClick, ColorChanged, ColorCheckboxChange}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import ReactCommonUtil.Implicits._
import io.suggest.ad.edit.m.edit.color.MColorPick
import io.suggest.color.MColorData
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 11:38
  * Description: React-компонент с галочкой выбора цвета.
  */
class ColorPickR(
                  lkAdEditCss     : LkAdEditCss,
                ) {

  type Props = ModelProxy[Option[MColorPick]]


  class Backend($: BackendScope[Props, _]) {

    /** Реакция на клики по галочке заливки цветом. */
    private def _onCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      dispatchOnProxyScopeCB($, ColorCheckboxChange(isChecked))
    }


    /** Реакция на настройку цвета. */
    private def _onColorChanged(color: Color, e: ReactEvent): Callback = {
      _onColorChangedBody(color, isComplete = false)
    }
    lazy val _onColorChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onColorChanged )


    /** Реакция на завершение выбора цвета. */
    private def _onColorCompletelyChanged(color: Color, e: ReactEvent): Callback = {
      _onColorChangedBody(color, isComplete = true)
    }
    lazy val _onColorCompletelyChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onColorCompletelyChanged )

    private def _onColorChangedBody(color: Color, isComplete: Boolean): Callback = {
      val mcd = MColorData(
        code = MColorData.stripDiez(color.hex)
      )
      dispatchOnProxyScopeCB($, ColorChanged(mcd, isCompleted = false))
    }


    /** Реакция на клик по кружочку цвета. */
    private def _onColorRoundClick(e: ReactMouseEvent): Callback = {
      dispatchOnProxyScopeCB($, ColorBtnClick)
    }


    def render(propsOptProxy: Props, pc: PropsChildren): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        val C = lkAdEditCss.BgColorOptPicker

        <.div(
          C.container,

          // Галочка для переключения между прозрачным фоном и заливкой цветом
          <.label(
            C.label,

            <.input(
              ^.`type` := HtmlConstants.Input.checkbox,
              ^.checked := props.colorOpt.nonEmpty,
              ^.onChange ==> _onCheckBoxChanged
            ),

            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),
            <.span(
              ^.`class` := Css.flat(Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR),
              pc
            )
          ),

          props.colorOpt.whenDefined { mColorData =>
            val colorHex = mColorData.hexCode

            <.span(
              ^.`class` := Css.flat(Css.CLICKABLE, Css.Display.INLINE_BLOCK),
              ^.onClick ==> ReactCommonUtil.stopPropagationCB,

              HtmlConstants.NBSP_STR,
              HtmlConstants.NBSP_STR,

              // Текущий цвет.
              <.div(
                C.colorRound,
                ^.backgroundColor := colorHex,
                ^.onClick ==> _onColorRoundClick
              ),

              if (props.pickS.isShown) {
                <.span(
                  Sketch(
                    new SketchProps {
                      override val color = colorHex
                      override val disableAlpha = true
                      override val onChange = _onColorChangedCbF
                      override val onChangeComplete = _onColorCompletelyChangedCbF
                      override val presetColors = {
                        props.colorsState.colorPresets
                          .iterator
                          .map { mcd =>
                            mcd.hexCode: PresetColor_t
                          }
                          .toJSArray
                      }
                    }
                  )
                )
              } else {
                EmptyVdom
              }

            )

          }

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props]("BgColor")
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(propsValOptProxy: Props)(children: VdomNode*) = component( propsValOptProxy )(children: _*)

}
