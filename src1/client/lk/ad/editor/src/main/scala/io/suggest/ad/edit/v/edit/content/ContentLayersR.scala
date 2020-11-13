package io.suggest.ad.edit.v.edit.content

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiSvgIcon, MuiToolTip, MuiToolTipProps}
import diode.{FastEq, UseValueEq}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.jd.edit.m.JdChangeLayer
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.common.BooleanUtil.Implicits._
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.raw.React
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.18 13:06
  * Description: Управление слоям. В Props приходит последняя координата path.
  */
class ContentLayersR(
                      contentLayerBtnR: ContentLayerBtnR
                    ) {

  case class PropsVal(
                       position : Int,
                       max      : Int,
                       isQdBl   : Boolean,
                     )
    extends UseValueEq
  @inline implicit def univEq: UnivEq[PropsVal] = UnivEq.derive


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Props_t]) {

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>

        def __mkBtn(btnProps: contentLayerBtnR.Props_t) =
          propsOptProxy.wrap(_ => btnProps)( contentLayerBtnR.apply )

        val canGoUp = props.position < props.max
        val canGoDown = props.position > 0

        val downValue = if (props.isQdBl) canGoUp else canGoDown
        val upValue = if (props.isQdBl) canGoDown else canGoUp

        val PP = contentLayerBtnR.PropsVal

        <.div(

          Messages( MsgCodes.`Layers` ),
          HtmlConstants.COLON,
          HtmlConstants.NBSP_STR,

          __mkBtn( PP.downMost(downValue) ),
          __mkBtn( PP.below(downValue) ),
          __mkBtn( PP.above(upValue) ),
          __mkBtn( PP.upperMost(upValue) ),

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( FastEqUtil.AnyValueEq ) )
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}



class ContentLayerBtnR {

  case class PropsVal(
                       titleMsgCode : String,
                       action       : JdChangeLayer,
                       icon         : MuiSvgIcon,
                       enabled      : Boolean,
                     )

  object PropsVal {
    def downMost(enabled: Boolean) =
      PropsVal(MsgCodes.`Downmost`, JdChangeLayer(up = false, bounded = true ), Mui.SvgIcons.ArrowDownward, enabled)
    def below(enabled: Boolean) =
      PropsVal(MsgCodes.`Below`, JdChangeLayer(up = false, bounded = false), Mui.SvgIcons.ExpandMore, enabled)
    def above(enabled: Boolean) =
      PropsVal(MsgCodes.`Above`, JdChangeLayer(up = true, bounded = false), Mui.SvgIcons.ExpandLess, enabled)
    def upperMost(enabled: Boolean) =
      PropsVal(MsgCodes.`Uppermost`, JdChangeLayer(up = true, bounded = true ), Mui.SvgIcons.ArrowUpward, enabled )
  }

  implicit object ContentLayerBtnPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.titleMsgCode ===* b.titleMsgCode) &&
      (a.action ==* b.action) &&
      (a.icon eq b.icon) &&
      (a.enabled ==* b.enabled)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private def _onBtnClick(action: JdChangeLayer)(e: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, action)
    }

    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value
      MuiToolTip(
        new MuiToolTipProps {
          override val title: React.Node = Messages( props.titleMsgCode )
        }
      )(
        <.span(
          MuiIconButton {
            val fn = ReactCommonUtil.cbFun1ToJsCb( _onBtnClick(props.action) )
            new MuiIconButtonProps {
              override val onClick  = fn
              override val disabled = !props.enabled
            }
          } (
            props.icon()()
          )
        )
      )
    }
  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(ContentLayerBtnPropsValFastEq) )
    .build

  def apply(propsProxy: Props) = component(propsProxy)

}
