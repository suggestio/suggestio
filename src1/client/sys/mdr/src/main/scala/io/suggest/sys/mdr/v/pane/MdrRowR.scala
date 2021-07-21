package io.suggest.sys.mdr.v.pane

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemProps, MuiListItemText, MuiSvgIcon, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.MMdrActionInfo
import io.suggest.sys.mdr.m.ApproveOrDismiss
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 16:32
  * Description: Один ряд в списке элементов на модерацию.
  */
class MdrRowR(
               mdrRowBtnR: MdrRowBtnR,
             ) {

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
                       itemIdOpt    : Option[Gid_t] = None,
                     )
  implicit object MdrRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.actionInfo ===* b.actionInfo) &&
      (a.mtgVariant ===* b.mtgVariant) &&
      (a.approveIcon eq b.approveIcon) &&
      (a.dismissIcon eq b.dismissIcon) &&
      (a.mdrPot      ==* b.mdrPot) &&
      (a.itemIdOpt   ==* b.itemIdOpt)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[PropsVal]


  class Backend($: BackendScope[Props, Props_t]) {

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
            propsProxy.wrap { _ =>
              mdrRowBtnR.PropsVal(true, props.actionInfo, props.approveIcon, MsgCodes.`Approve`, isDisabled, props.itemIdOpt)
            }( mdrRowBtnR.component.apply ),

            // Заголовок ряда
            children,

            // Кнопка "отказать"
            propsProxy.wrap { _ =>
              mdrRowBtnR.PropsVal(false, props.actionInfo, props.dismissIcon, MsgCodes.`Refuse`, isDisabled, props.itemIdOpt)
            }( mdrRowBtnR.component.apply ),

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
        },

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}


/** Компонент кнопки. */
class MdrRowBtnR {

  case class PropsVal(
                       isApprove  : Boolean,
                       actionInfo : MMdrActionInfo,
                       icon       : MuiSvgIcon,
                       msgCode    : String,
                       isDisabled : Boolean,
                       itemIdOpt  : Option[Gid_t],
                     )
  implicit object MdrRowBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.isApprove ==* b.isApprove) &&
      (a.actionInfo ===* b.actionInfo) &&
      (a.icon eq b.icon) &&
      (a.msgCode ===* b.msgCode) &&
      (a.isDisabled ==* b.isDisabled) &&
      (a.itemIdOpt ===* b.itemIdOpt)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {

    /** callback клика по item'у. */
    private def onApproveOrDismissBtnClick(info: MMdrActionInfo, isApprove: Boolean)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ApproveOrDismiss(info, isApprove) )


    def render(p: Props_t): VdomElement = {
      val hintSuffixNodes = p.itemIdOpt.fold {
        List.empty[VdomNode]
      } { itemId =>
        // Надо отрендерить инфу по item'у в зависимости от типа item'а.
        List[VdomNode](
          HtmlConstants.SPACE,
          Messages( MsgCodes.`_N` ),
          itemId
        )
      }
      val titleNode = (
        (Messages( p.msgCode ): VdomNode) ::
        hintSuffixNodes
      )
        .toVdomArray

      MuiToolTip {
        new MuiToolTipProps {
          override val title = titleNode.rawNode
          override val placement = MuiToolTipPlacements.BottomStart
        }
      } (
        <.span(
          MuiIconButton {
            val _onClickCbF = ReactCommonUtil.cbFun1ToJsCb(
              onApproveOrDismissBtnClick( p.actionInfo, isApprove = p.isApprove )
            )
            new MuiIconButtonProps {
              override val onClick = _onClickCbF
              override val disabled = p.isDisabled
            }
          } (
            p.icon()()
          )
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( MdrRowBtnRPropsValFastEq ) )
    .build

}
