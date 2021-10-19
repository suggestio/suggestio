package io.suggest.bill.cart.v.itm

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.m.CartDeleteBtnClick
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._
import ReactCommonUtil.Implicits._
import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiToolBar, MuiToolBarClasses, MuiToolBarProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.18 15:27
  * Description: Order items toolbar for actions.
  */
class ItemsDeleteSelectedR(
                         orderCss     : OrderCss,
                         crCtxP       : React.Context[MCommonReactCtx],
                   ) {

  case class PropsVal(
                       countSelected  : Int,
                       isPendingReq   : Boolean
                     )
  implicit object ItemsToolBarRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.countSelected ==* b.countSelected) &&
      (a.isPendingReq ==* b.isPendingReq)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    /** Delete button clicked. */
    lazy val _onDeleteBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      dispatchOnProxyScopeCB( $, CartDeleteBtnClick )
    }


    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        // If selected (checked) items non-empty, render count selected:
        ReactCommonUtil.maybeEl( props.countSelected > 0 ) {
          React.Fragment(

            <.div(
              ^.key := "t",
              orderCss.ItemsTable.ToolBar.title,

              // Selected items count:
              MuiTypoGraphy.component.withKey("n")(
                new MuiTypoGraphyProps {
                  override val variant = MuiTypoGraphyVariants.subtitle1
                  override val noWrap = true
                }
              )(
                crCtxP.message( MsgCodes.`N.selected`, props.countSelected )
              )
            ),

            <.div(
              ^.key := "s",
              orderCss.ItemsTable.ToolBar.spacer
            ),

            // Delete button:
            MuiButton(
              new MuiButtonProps {
                override val startIcon = Mui.SvgIcons.Delete()().raw
                override val onClick = _onDeleteBtnClickJsCbF
                override val disabled = props.isPendingReq
              }
            )(
              crCtxP.message( MsgCodes.`Delete` )
            ),

          )
        }

      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .stateless
    .renderBackend[Backend]
    .build

}
