package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.ad.edit.m.RotateSet
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
import io.suggest.lk.r.{InputSliderR, LkCheckBoxR}
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 22:14
  * Description: Компонент галочки и слайдера ротации.
  */
class RotateR(
               lkAdEditCss      : LkAdEditCss,
               inputSliderR     : InputSliderR,
               lkCheckBoxR      : LkCheckBoxR,
             ) {

  import lkCheckBoxR.LkCheckBoxRFastEq

  case class PropsVal(
                       value: Option[Int]
                     )
  implicit object RotateRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.value ==* b.value
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          // Галочка активации/деактивации вращения.
          propsOptProxy.wrap { _ =>
            lkCheckBoxR.PropsVal(
              label = Messages( MsgCodes.`Rotation` ),
              checked = props.value.isDefined,
              onChange = { isEnabled =>
                val rotateDeg = OptionUtil.maybe(isEnabled)(0)
                RotateSet(rotateDeg)
              }
            )
          }( lkCheckBoxR.apply ),

          HtmlConstants.NBSP_STR,

          // Слайдер градусов вращения.
          propsOptProxy.wrap { _ =>
            for (v <- props.value) yield {
              inputSliderR.PropsVal(
                min       = -JdConst.ROTATE_MAX_ABS,
                max       = JdConst.ROTATE_MAX_ABS,
                value     = v,
                onChange  = { newValue => RotateSet(Some(newValue)) }
              )
            }
          }( inputSliderR.apply ),

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def _apply(propsValOptProxy: Props) = component(propsValOptProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
