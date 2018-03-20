package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.RotateSet
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 22:14
  * Description: Компонент галочки и слайдера ротации.
  */
class RotateR(
               lkAdEditCss: LkAdEditCss
             ) {

  case class PropsVal(
                       value: Option[Int]
                     )
  implicit object RotateRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.value ==* b.value
    }
  }

  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, Unit]) {

    private def onCheckboxChange(e: ReactEventFromInput): Callback = {
      val isEnabled = e.target.checked
      val rotateDeg = OptionUtil.maybe(isEnabled)(0)
      dispatchOnProxyScopeCB($, RotateSet(rotateDeg))
    }

    /** Реакция на движение слайдера градусов наклона. */
    private def onValueChange(e: ReactEventFromInput): Callback = {
      val v = e.target.value
      if (v.isEmpty) {
        Callback.empty
      } else {
        val newValue = v.toInt
        dispatchOnProxyScopeCB($, RotateSet(Some(newValue)) )
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          // Галочка активации/деактивации вращения.
          <.label(
            ^.`class` := Css.CLICKABLE,

            <.input(
              ^.`type` := HtmlConstants.Input.checkbox,
              ^.checked := props.value.isDefined,
              ^.onChange ==> onCheckboxChange
            ),

            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),

            <.span(
              ^.`class` := Css.flat( Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR ),
              Messages( MsgCodes.`Rotation` )
            )
          ),

          HtmlConstants.NBSP_STR,

          // Слайдер градусов вращения.
          props.value.whenDefined { value =>
            <.span(
              <.input(
                lkAdEditCss.RangeInput.rangeSlider,
                ^.`type` := HtmlConstants.Input.range,
                ^.value  := value,
                ^.min := -JdConst.ROTATE_MAX_ABS,
                ^.max := JdConst.ROTATE_MAX_ABS,
                ^.onChange ==> onValueChange
              ),

              HtmlConstants.NBSP_STR,

              <.span(
                ^.`class` := Css.Input.INPUT,
                <.input(
                  lkAdEditCss.RangeInput.rangeText,
                  ^.`type`    := HtmlConstants.Input.text,
                  ^.value     := value,
                  ^.onChange  ==> onValueChange
                )
              )
            )

          }

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props]("Rotate")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
