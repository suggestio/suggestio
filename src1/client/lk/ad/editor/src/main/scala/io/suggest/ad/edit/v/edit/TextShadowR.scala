package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.{SetBlurTextShadow, SetHorizOffTextShadow, SetTextShadowEnabled, SetVertOffTextShadow}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.MsgCodes
import io.suggest.jd.JdConst
import io.suggest.jd.tags.MJdShadow
import io.suggest.lk.r.{InputSliderR, LkCheckBoxR}
import io.suggest.lk.r.color.ColorCheckBoxR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import scalacss.ScalaCssReact._
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
                   lkAdEditCss      : LkAdEditCss,
                 ) {


  type Props_t = Option[MJdShadow]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisible     : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsOptProxy: Props, s: State): VdomElement = {
      <.div(
        lkAdEditCss.TextShadow.cont,

        // Галочка включения тени.
        {
          val label = Messages( MsgCodes.`Text.shadow` ): VdomNode
          val onChange = SetTextShadowEnabled.apply _
          propsOptProxy.wrap { propsOpt =>
            val p = lkCheckBoxR.PropsVal(
              label    = label,
              checked  = propsOpt.isDefined,
              onChange = onChange
            )
            Some(p): lkCheckBoxR.Props_t
          }( lkCheckBoxR.apply )(implicitly, OptFastEq.Wrapped(lkCheckBoxR.LkCheckBoxRFastEq))
        },

        // Управление параметрами тени:
        {
          import JdConst.Shadow.{TextShadow => C}
          lazy val sliderPropsOptFeq = OptFastEq.Wrapped( inputSliderR.InputSliderRPropsValFastEq )

          // Горизонтальный сдвиг
          lazy val horizOff = {
            val onChange = SetHorizOffTextShadow.apply _
            val css = Some( lkAdEditCss.TextShadow.first.htmlClass )
            propsOptProxy.wrap { propsOpt =>
              for (props <- propsOpt) yield {
                val h = C.HORIZ_OFFSET_MIN_MAX
                inputSliderR.PropsVal(
                  min       = -h,
                  max       = h,
                  value     = props.hOffset,
                  onChange  = onChange,
                  css       = css,
                )
              }
            }( inputSliderR.apply )(implicitly, sliderPropsOptFeq)
          }

          // Вертикальный сдвиг
          lazy val vertOff = {
            val onChange = SetVertOffTextShadow.apply _
            val css = Some( lkAdEditCss.TextShadow.second.htmlClass )
            propsOptProxy.wrap { propsOpt =>
              for (props <- propsOpt) yield {
                val v = C.VERT_OFFSET_MIN_MAX
                inputSliderR.PropsVal(
                  min       = -v,
                  max       = v,
                  value     = props.vOffset,
                  onChange  = onChange,
                  css       = css,
                )
              }
            }( inputSliderR.apply )(implicitly, sliderPropsOptFeq)
          }

          // Блюр тени:
          lazy val blur = {
            val onChange = SetBlurTextShadow.apply _
            val css = Some( lkAdEditCss.TextShadow.third.htmlClass )
            propsOptProxy.wrap { propsOpt =>
              for (props <- propsOpt) yield {
                inputSliderR.PropsVal(
                  min       = 0,
                  max       = C.BLUR_MAX * C.BLUR_FRAC,
                  value     = props.blur.getOrElse(0),
                  onChange  = onChange,
                  css       = css,
                )
              }
            }( inputSliderR.apply )(implicitly, sliderPropsOptFeq)
          }

          // Цвет тени:
          lazy val shadColor = {
            val marker = Some( MJdShadow.ColorMarkers.TextShadow )
            val label  = Messages( MsgCodes.`Shadow.color` ): VdomNode
            propsOptProxy.wrap { propsOpt =>
              for (props <- propsOpt) yield {
                colorCheckBoxR.PropsVal(
                  color  = props.color,
                  label  = label,
                  marker = marker,
                )
              }
            }( colorCheckBoxR.apply )(implicitly, OptFastEq.Wrapped(colorCheckBoxR.ColorCheckBoxPropsValFastEq))
          }

          s.isVisible { isVisibleSomeProxy =>
            ReactCommonUtil.maybeEl( isVisibleSomeProxy.value.value ) {
              <.div(
                horizOff,
                vertOff,
                blur,
                shadColor,
              )
            }
          }

        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isVisible = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.isDefined )
        }( FastEq.AnyRefEq )
      )
    }
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
