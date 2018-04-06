package io.suggest.lk.r.color

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.css.Css
import io.suggest.lk.m.ColorBtnClick
import io.suggest.model.n2.node.meta.colors.MColorType
import japgolly.scalajs.react.{BackendScope, Callback, ReactMouseEvent, ScalaComponent}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 22:48
  * Description: React-компонент кнопки открытия color-picker'а для выбора цвета.
  */
object ColorBtnR {

  def defaultCssClasses = Css.Lk.COLOR :: Css.Display.INLINE_BLOCK :: Css.CLICKABLE :: Nil

}

class ColorBtnR {

  case class PropsVal(
                       color        : MColorData,
                       colorType    : Option[MColorType] = None,
                       cssClass     : Option[String] = None
                     )
  implicit object ColorBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
        (a.cssClass ===* b.cssClass)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клик по кружочку цвета. */
    private def _onColorBtnClick(e: ReactMouseEvent): Callback = {
      val fixedCoord = MCoords2di(
        x = e.clientX.toInt,
        y = e.clientY.toInt
      )
      ReactCommonUtil.stopPropagationCB(e) >> dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        val colorTypeOpt = propsProxy.value.flatMap(_.colorType)
        ColorBtnClick(fixedCoord, colorTypeOpt)
      }
    }

    def render(p: Props): VdomElement = {
      p.value.whenDefinedEl { props =>
        <.div(
          ^.`class` := props.cssClass.getOrElse {
            Css.flat1( ColorBtnR.defaultCssClasses )
          },
          ^.backgroundColor := props.color.hexCode,
          ^.onClick ==> _onColorBtnClick
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ColorBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def _apply(propsValOptProxy: Props) = component( propsValOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
