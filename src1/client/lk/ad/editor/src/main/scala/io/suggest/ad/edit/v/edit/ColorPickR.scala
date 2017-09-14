package io.suggest.ad.edit.v.edit

import com.github.casesandberg.react.color._
import diode.react.ModelProxy
import io.suggest.ad.edit.m.edit.MColorPick
import io.suggest.ad.edit.m.{ColorChanged, ColorCheckboxChange, ColorBtnClick}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.react.ReactCommonUtil
import ReactCommonUtil.Implicits._
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

    private def _onCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      dispatchOnProxyScopeCB($, ColorCheckboxChange(isChecked))
    }


    private def _onColorChanged(color: Color, e: ReactEvent): Callback = {
      _onColorChangedBody(color, isComplete = false)
    }
    lazy val _onColorChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onColorChanged )

    private def _onColorCompletelyChanged(color: Color, e: ReactEvent): Callback = {
      _onColorChangedBody(color, isComplete = true)
    }
    lazy val _onColorCompletelyChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onColorCompletelyChanged )

    private def _onColorChangedBody(color: Color, isComplete: Boolean): Callback = {
      val mcd = MColorData.stripingDiez(color.hex)
      dispatchOnProxyScopeCB($, ColorChanged(mcd, isCompleted = false))
    }


    private def _onColorRoundClick: Callback = {
      dispatchOnProxyScopeCB($, ColorBtnClick)
    }


    def render(propsOptProxy: Props, pc: PropsChildren): VdomElement = {
      val propsOpt = propsOptProxy.value
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

              HtmlConstants.NBSP_STR,
              HtmlConstants.NBSP_STR,

              // Текущий цвет.
              <.div(
                C.colorRound,
                ^.backgroundColor := colorHex,
                ^.onClick --> _onColorRoundClick
              ),

              if (props.pickS.isShown) {
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