package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemText, MuiSvgIcon, MuiTypoGraphy, MuiTypoGraphyProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.{ApproveOrDismiss, MMdrActionInfo}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

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
                       dismissIcon  : MuiSvgIcon
                     )
  implicit object MdrRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.actionInfo ===* b.actionInfo) &&
      (a.mtgVariant ===* b.mtgVariant) &&
      (a.approveIcon eq b.approveIcon) &&
      (a.dismissIcon eq b.dismissIcon)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[PropsVal]


  class Backend($: BackendScope[Props, Props_t]) {
    /** callback клика по item'у. */
    private def onApproveOrDismissBtnClick(info: MMdrActionInfo, isApprove: Boolean)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ApproveOrDismiss(info, isApprove) )

    /** Кнопка ряда. */
    private def __rowBtn(actionInfo: MMdrActionInfo, icon: MuiSvgIcon): VdomElement = {
      MuiIconButton(
        new MuiIconButtonProps {
          override val onClick: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.defined {
            ReactCommonUtil.cbFun1ToJsCb(
              onApproveOrDismissBtnClick( actionInfo, isApprove = true ) )
          }
        }
      )(
        icon()()
      )
    }


    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      val props = propsProxy.value

      MuiListItem()(
        MuiListItemText()(
          MuiTypoGraphy(
            new MuiTypoGraphyProps {
              override val variant = props.mtgVariant
            }
          )(
            // Кнопка "подтвердить"
            __rowBtn( props.actionInfo, props.approveIcon),

            children,

            // Кнопка "отказать"
            __rowBtn( props.actionInfo, props.dismissIcon )
          )
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
