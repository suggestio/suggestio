package io.suggest.bill.cart.v.itm

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.m.CartDeleteBtnClick
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._
import ReactCommonUtil.Implicits._
import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiToolBar, MuiToolBarClasses, MuiToolBarProps, MuiToolTip, MuiToolTipPlacements, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.18 15:27
  * Description: Order items toolbar for actions.
  */
class ItemsToolBarR(
                     orderCss: OrderCss
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
        val tbCss = new MuiToolBarClasses {
          override val root = orderCss.ItemsTable.ToolBar.root.htmlClass
        }

        MuiToolBar(
          new MuiToolBarProps {
            override val classes = tbCss
            override val disableGutters = true
          }
        )(
          // If selected (checked) items non-empty, render count selected:
          ReactCommonUtil.maybeNode( props.countSelected > 0 ) {
            VdomArray(

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
                  Messages( MsgCodes.`N.selected`, props.countSelected )
                )
              ),

              <.div(
                ^.key := "s",
                orderCss.ItemsTable.ToolBar.spacer
              ),

              // Delete button:
              MuiToolTip.component.withKey("d")(
                new MuiToolTipProps {
                  override val title: React.Node = Messages( MsgCodes.`Delete` )
                  override val placement = MuiToolTipPlacements.Top
                }
              )(
                MuiIconButton(
                  new MuiIconButtonProps {
                    override val onClick = _onDeleteBtnClickJsCbF
                    override val disabled = props.isPendingReq
                  }
                )(
                  Mui.SvgIcons.Delete()()
                )
              )

            )
          }

        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
