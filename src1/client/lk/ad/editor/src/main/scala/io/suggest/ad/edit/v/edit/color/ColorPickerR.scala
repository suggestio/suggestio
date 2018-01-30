package io.suggest.ad.edit.v.edit.color

import com.github.casesandberg.react.color.{Color, PresetColor_t, Sketch, SketchProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.ColorChanged
import io.suggest.ad.edit.m.edit.color.MColorsState
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

import scala.scalajs.js.JSConverters._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 21:21
  * Description: Компонент непосредственного color-picker'а.
  * Раньше жил прямо внутри [[ColorCheckboxR]], но с появлением аккордеона возникли проблемы:
  * выпадающий color picker залезает под нижнюю границу блока, делая его бесполезным.
  *
  * Решено отвязать его от чекбокса.
  */
class ColorPickerR(
                    lkAdEditCss: LkAdEditCss
                  ) {

  case class PropsVal(
                       color          : MColorData,
                       colorsState    : MColorsState,
                       fixedXy        : MCoords2di
                     )
  implicit object ColorPickerPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
        (a.colorsState ===* b.colorsState) &&
        (a.fixedXy ===* b.fixedXy)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на настройку цвета. */
    private def _onColorChanged(color: Color, e: ReactEvent): Callback = {
      _onColorChangedBody(color, isComplete = false)
    }
    private lazy val _onColorChangedCbF = ReactCommonUtil.cbFun2ToJsCb( _onColorChanged )

    private def _onColorChangedBody(color: Color, isComplete: Boolean): Callback = {
      val mcd = MColorData(
        code = MColorData.stripDiez(color.hex)
      )
      dispatchOnProxyScopeCB($, ColorChanged(mcd, isCompleted = isComplete))
    }


    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          lkAdEditCss.BgColorOptPicker.pickerCont,
          ^.left := props.fixedXy.x.px,
          ^.top := props.fixedXy.y.px,

          // Чтобы не скрывался picker из-за DocBodyClick.
          ^.onClick ==> ReactCommonUtil.stopPropagationCB,

          Sketch(
            new SketchProps {
              override val color        = props.color.hexCode
              override val disableAlpha = true
              override val onChange     = _onColorChangedCbF
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
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ColorPick")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
