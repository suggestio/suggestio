package io.suggest.ad.edit.v.edit.color

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.{ColorBtnClick, ColorCheckboxChange}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.Element

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 11:38
  * Description: React-компонент с галочкой выбора цвета.
  */
class ColorCheckboxR(
                      lkAdEditCss     : LkAdEditCss,
                    ) {

  /** Модель пропертисов этого компонента.
    *
    * @param color Цвет.
    *              None значит прозрачный.
    */
  case class PropsVal(
                       color          : Option[MColorData]
                     )
  implicit object ColorCheckboxPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.color ===* b.color
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, _]) {

    /** Реакция на клики по галочке заливки цветом. */
    private def _onCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      dispatchOnProxyScopeCB($, ColorCheckboxChange(isChecked))
    }

    /** Реакция на клик по кружочку цвета. */
    private def _onColorRoundClick(e: ReactMouseEvent): Callback = {
      val fixedCoord = MCoords2di(
        x = e.clientX.toInt,
        y = e.clientY.toInt
      )

      dispatchOnProxyScopeCB($, ColorBtnClick(fixedCoord))
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
              ^.checked := props.color.nonEmpty,
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

          props.color.whenDefined { mColorData =>
            <.span(
              ^.`class` := Css.flat(Css.CLICKABLE, Css.Display.INLINE_BLOCK),
              ^.onClick ==> ReactCommonUtil.stopPropagationCB,

              HtmlConstants.NBSP_STR,
              HtmlConstants.NBSP_STR,

              // Текущий цвет.
              <.div(
                C.colorRound,
                ^.backgroundColor := mColorData.hexCode,
                ^.onClick ==> _onColorRoundClick
              )

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
