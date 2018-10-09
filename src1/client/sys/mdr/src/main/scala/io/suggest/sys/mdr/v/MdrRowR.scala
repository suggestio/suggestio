package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemProps, MuiListItemText, MuiSvgIcon, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.MMdrActionInfo
import io.suggest.sys.mdr.m.ApproveOrDismiss
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.raw.React
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 16:32
  * Description: Один ряд в списке элементов на модерацию.
  */
class MdrRowR {

  /** Модель пропертисов компонента.
    *
    * @param actionInfo Данные экшена.
    * @param mtgVariant вариант MuiTypoGraphy
    * @param approveIcon Иконка аппрува.
    * @param dismissIcon Иконка отказа.
    */
  case class PropsVal(
                       actionInfo   : MMdrActionInfo,
                       mtgVariant   : String,
                       approveIcon  : MuiSvgIcon,
                       dismissIcon  : MuiSvgIcon,
                       mdrPot       : Pot[None.type],
                     )
  implicit object MdrRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.actionInfo ===* b.actionInfo) &&
      (a.mtgVariant ===* b.mtgVariant) &&
      (a.approveIcon eq b.approveIcon) &&
      (a.dismissIcon eq b.dismissIcon) &&
      (a.mdrPot      ==* b.mdrPot)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[PropsVal]


  class Backend($: BackendScope[Props, Props_t]) {
    /** callback клика по item'у. */
    private def onApproveOrDismissBtnClick(info: MMdrActionInfo, isApprove: Boolean)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ApproveOrDismiss(info, isApprove) )

    /** Кнопка ряда. */
    private def __rowBtn(isApprove: Boolean, actionInfo: MMdrActionInfo, icon: MuiSvgIcon, msgCode: String, isDisabled: Boolean): VdomElement = {
      MuiToolTip(
        new MuiToolTipProps {
          override val title: React.Node = Messages( msgCode )
          override val placement = MuiToolTipPlacements.BottomStart
        }
      )(
        MuiIconButton {
          val _onClickCbF = ReactCommonUtil.cbFun1ToJsCb(
            onApproveOrDismissBtnClick( actionInfo, isApprove = isApprove )
          )
          new MuiIconButtonProps {
            override val onClick = _onClickCbF
            override val disabled = isDisabled
          }
        } (
          icon()()
        )
      )
    }


    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      val props = propsProxy.value
      val isDisabled = props.mdrPot.isPending

      MuiListItem(
        new MuiListItemProps {
          override val disableGutters = true
          override val dense = true
        }
      )(
        MuiListItemText()(
          MuiTypoGraphy(
            new MuiTypoGraphyProps {
              override val variant = props.mtgVariant
            }
          )(
            // Кнопка "подтвердить"
            __rowBtn( true, props.actionInfo, props.approveIcon, MsgCodes.`Approve`, isDisabled ),

            children,

            // Кнопка "отказать"
            __rowBtn( false, props.actionInfo, props.dismissIcon, MsgCodes.`Refuse`, isDisabled )
          )
        ),

        // Отрендерить ошибку, если она случилась.
        props.mdrPot.renderFailed { ex =>
          val errMsg = VdomArray(
            Messages( MsgCodes.`Error` ),
            HtmlConstants.COLON,
            HtmlConstants.SPACE,
            ex.toString()
          )
          MuiToolTip(
            new MuiToolTipProps {
              override val title: React.Node = errMsg.rawNode
            }
          )(
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val color = MuiColorTypes.secondary
              }
            )(
              Mui.SvgIcons.SmsFailed()()
            )
          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
