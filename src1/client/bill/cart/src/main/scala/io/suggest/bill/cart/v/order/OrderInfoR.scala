package io.suggest.bill.cart.v.order

import com.materialui.{Mui, MuiCard, MuiCardHeader, MuiIconButton, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.bill.cart.m.UnHoldOrderDialogOpen
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.18 11:02
  * Description: One order meta-data display component.
  * For cart-order this is mostly useless; only for archived or other orders.
  */
class OrderInfoR(
                  crCtxP      : React.Context[MCommonReactCtx],
                ) {

  type Props_t = Option[MOrder]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    private lazy val _onCancelOrderClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, UnHoldOrderDialogOpen( true ) )
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { morder =>
        MuiCard()(
          MuiCardHeader(
            new MuiCardHeader.Props {

              override val action = {
                val _actionBtnMaybe: VdomElement = if (morder.status ==* MOrderStatuses.Hold) {
                  // Show cancel/unholding button.
                  MuiToolTip(
                    new MuiToolTipProps {
                      override val title = crCtxP.message( MsgCodes.`Cancel.order...` ).rawNode
                    }
                  )(
                    MuiIconButton(
                      new MuiIconButtonProps {
                        override val onClick = _onCancelOrderClick
                        // TODO override val disabled =
                      }
                    )(
                      Mui.SvgIcons.Cancel()()
                    )
                  )
                } else {
                  ReactCommonUtil.VdomNullElement
                }
                _actionBtnMaybe.rawNode
              }

              // Header: - Order #3343 | or Cart | or ...
              override val title = crCtxP
                .consume { crCtx =>
                  morder.id
                    .fold {
                      crCtx.messages( MsgCodes.`Cart` )
                    } { orderId =>
                      crCtx.messages( MsgCodes.`Order.N`, orderId.toDouble )
                    }
                }
                .rawNode

              // Order status with date of last status change.
              override val subheader = React.Fragment(
                crCtxP.message( morder.status.singular ),

                HtmlConstants.SPACE,
                HtmlConstants.PIPE,
                HtmlConstants.SPACE,

                // Date of order's status change.
                YmdR(
                  morder.dateStatus.toLocalDate.toYmd
                )(),
              )
                .rawNode
            }
          ),
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .stateless
    .renderBackend[Backend]
    .build

}
