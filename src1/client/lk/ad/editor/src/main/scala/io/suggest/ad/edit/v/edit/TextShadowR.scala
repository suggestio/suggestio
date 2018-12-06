package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.{SetBlurTextShadow, SetHorizOffTextShadow, SetTextShadowEnabled, SetVertOffTextShadow}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
import io.suggest.jd.tags.MJdShadow
import io.suggest.lk.r.{InputSliderR, LkCheckBoxR}
import io.suggest.lk.r.color.ColorCheckBoxR
import io.suggest.msg.Messages
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.18 18:06
  * Description: Компонент для редактора тени.
  */
class TextShadowR(
                   val inputSliderR : InputSliderR,
                   colorCheckBoxR   : ColorCheckBoxR,
                   lkCheckBoxR      : LkCheckBoxR,
                 ) {

  import lkCheckBoxR.LkCheckBoxRFastEq

  case class PropsVal(
                       jdShadow: MJdShadow
                     )
  implicit object TextShadowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.jdShadow ===* b.jdShadow)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsOptProxy: Props): VdomElement = {
      val propsOpt = propsOptProxy.value

      <.div(

        // Галочка включения тени.
        propsOptProxy.wrap { _ =>
          lkCheckBoxR.PropsVal(
            label = Messages( MsgCodes.`Text.shadow` ),
            checked = propsOpt.isDefined,
            onChange = SetTextShadowEnabled(_)
          )
        }( lkCheckBoxR.apply ),

        // Управление параметрами тени:
        propsOpt.whenDefined { props =>
          val C = JdConst.Shadow.TextShadow
          <.div(

            // Горизонтальный сдвиг
            propsOptProxy.wrap { _ =>
              val h = C.HORIZ_OFFSET_MIN_MAX
              val slProps = inputSliderR.PropsVal(
                min = -h,
                max = h,
                value = props.jdShadow.hOffset,
                onChange = SetHorizOffTextShadow(_)
              )
              Some(slProps): inputSliderR.Props_t
            }( inputSliderR.apply ),

            // Вертикальный сдвиг
            propsOptProxy.wrap { _ =>
              val v = C.VERT_OFFSET_MIN_MAX
              val slProps = inputSliderR.PropsVal(
                min = -v,
                max = v,
                value = props.jdShadow.vOffset,
                onChange = SetVertOffTextShadow(_)
              )
              Some(slProps): inputSliderR.Props_t
            }( inputSliderR.apply ),

            // Блюр тени:
            propsOptProxy.wrap { _ =>
              val slProps = inputSliderR.PropsVal(
                min = 0,
                max = C.BLUR_MAX * C.BLUR_FRAC,
                value = props.jdShadow.blur.getOrElse(0),
                onChange = SetBlurTextShadow(_)
              )
              Some(slProps): inputSliderR.Props_t
            }( inputSliderR.apply ),

            // Цвет тени:
            propsOptProxy.wrap { _ =>
              val p = colorCheckBoxR.PropsVal(
                color  = props.jdShadow.color,
                label  = Messages( MsgCodes.`Shadow.color` ),
                marker = Some( MJdShadow.ColorMarkers.TextShadow )
              )
              Some(p): colorCheckBoxR.Props_t
            }( colorCheckBoxR.apply )

          )
        },


      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
