package io.suggest.bill.cart.v.order

import com.materialui.{MuiCard, MuiCardContent, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.mbill2.m.order.MOrder
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.dt.CommonDateTimeUtil.Implicits._

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

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { morder =>
        MuiCard()(
          MuiCardContent()(

            // Header: - Order #3343 | or Cart | or ...
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.h5
              }
            )(
              crCtxP.consume { crCtx =>
                morder.id
                  .fold {
                    crCtx.messages( MsgCodes.`Cart` )
                  } { orderId =>
                    crCtx.messages( MsgCodes.`Order.N`, orderId )
                  }
              }
            ),

            // Order status with date of last status change.
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.caption
              }
            )(
              crCtxP.message( morder.status.singular ),

              HtmlConstants.SPACE,
              HtmlConstants.PIPE,
              HtmlConstants.SPACE,

              // Date of order's status change.
              YmdR(
                morder.dateStatus.toLocalDate.toYmd
              )(),
            )

          )
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
