package io.suggest.lk.r.color

import com.github.casesandberg.react.color.{Color, PresetColor_t, Sketch, SketchProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.lk.m.ColorChanged
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 21:21
  * Description: Компонент непосредственного color-picker'а.
  *
  * Решено отвязать его от чекбокса.
  */
class ColorPickerR {

  case class PropsVal(
                       color          : MColorData,
                       colorPresets   : List[MColorData]    = Nil,
                       cssClass       : Option[String]      = None,
                       topLeftPx      : Option[MCoords2di]  = None,
                     )
  implicit object ColorPickerPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
      (a.colorPresets ===* b.colorPresets) &&
      (a.cssClass ===* b.cssClass) &&
      OptFastEq.Plain.eqv(a.topLeftPx, b.topLeftPx)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


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
      ReactDiodeUtil.dispatchOnProxyScopeCB($, ColorChanged(mcd, isCompleted = isComplete))
    }


    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          props.cssClass.whenDefined { cssClass =>
            ^.`class` := cssClass
          },

          props.topLeftPx.whenDefined { xy =>
            TagMod(
              ^.left := xy.x.px,
              ^.top  := xy.y.px
            )
          },

          // Чтобы не скрывался picker из-за DocBodyClick.
          ^.onClick ==> ReactCommonUtil.stopPropagationCB,

          Sketch(
            new SketchProps {
              override val color        = props.color.hexCode
              override val disableAlpha = true
              override val onChange     = _onColorChangedCbF
              override val presetColors = {
                props.colorPresets
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
