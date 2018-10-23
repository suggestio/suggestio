package io.suggest.sys.mdr.v.toolbar

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiSvgIcon, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.MdrNextNode
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.18 18:28
  * Description: Базовый класс для какой-то не-href кнопки на панели кнопок.
  */
class MdrTbStepBtnR {

  final case class PropsVal(
                             titleMsgCode   : String,
                             icon           : MuiSvgIcon,
                             isDisabled     : Boolean,
                             offsetDelta    : Int,
                           )
  object PropsVal {
    def ToBeginning(isDisabled: Boolean, offsedDelta: Int): PropsVal =
      PropsVal(MsgCodes.`To.beginning`, Mui.SvgIcons.FastRewind, isDisabled, offsedDelta)
    def PreviousNode(isDisabled: Boolean): PropsVal =
      PropsVal(MsgCodes.`Previous.node`, Mui.SvgIcons.SkipPrevious, isDisabled, -1)
    def Refresh(isDisabled: Boolean = false): PropsVal =
      PropsVal(MsgCodes.`Reload`, Mui.SvgIcons.Refresh, isDisabled, 0)
    def NextNode(isDisabled: Boolean): PropsVal =
      PropsVal(MsgCodes.`Next.node`, Mui.SvgIcons.SkipNext, isDisabled, +1)
    def ToEnd(queueLen: Option[Int]): PropsVal =
      PropsVal(MsgCodes.`To.end`, Mui.SvgIcons.FastForward, queueLen.isEmpty, queueLen.getOrElse(0))

    @inline implicit def univEq: UnivEq[PropsVal] = UnivEq.force
  }
  implicit object MdrPanelStepBtnPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.titleMsgCode ===* b.titleMsgCode) &&
      (a.icon eq b.icon) &&
      (a.isDisabled ==* b.isDisabled) &&
      (a.offsetDelta ==* b.offsetDelta)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _btnClick(e: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        MdrNextNode(offsetDelta = props.value.offsetDelta)
      }
    }
    private val _btnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _btnClick )


    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value

      MuiToolTip {
        new MuiToolTipProps {
          override val title: React.Node = Messages( p.titleMsgCode )
        }
      } (
        MuiIconButton(
          new MuiIconButtonProps {
            override val onClick  = _btnClickJsCbF
            override val disabled = p.isDisabled
          }
        )(
          p.icon()()
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def _apply( propsProxy: Props ) = component(propsProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
