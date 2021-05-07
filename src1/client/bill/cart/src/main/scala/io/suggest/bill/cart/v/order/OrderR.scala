package io.suggest.bill.cart.v.order

import com.materialui.MuiTable
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.itm.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR, ItemsToolBarR}
import io.suggest.bill.cart.v.txn.TxnsR
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.jd.render.v.{JdCss, JdCssStatic}
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:20
  * Description: Cart/order Form root component.
  */
class OrderR(
              cartCss                : OrderCss,
              jdCssStatic            : JdCssStatic,
              val itemRowR           : ItemRowR,
              val itemsTableHeadR    : ItemsTableHeadR,
              val itemsTableBodyR    : ItemsTableBodyR,
              val itemsToolBarR      : ItemsToolBarR,
              val goToPayBtnR        : GoToPayBtnR,
              val orderInfoR         : OrderInfoR,
              val txnsR              : TxnsR,
            ) {


  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    jdCssC          : ReactConnectProxy[JdCss],
                    orderHeadC      : ReactConnectProxy[itemsTableHeadR.Props_t],
                    orderBodyC      : ReactConnectProxy[itemsTableBodyR.Props_t],
                    toolBarPropsC   : ReactConnectProxy[itemsToolBarR.Props_t],
                    goToPayPropsC   : ReactConnectProxy[goToPayBtnR.Props_t],
                    orderOptC       : ReactConnectProxy[orderInfoR.Props_t],
                    txnsPricedOptC  : ReactConnectProxy[txnsR.Props_t],
                  )

  /** Root component core. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Short order description, if any.
        s.orderOptC { orderInfoR.component.apply },

        // Static cart form CSS styles:
        propsProxy.wrap(_ => cartCss)( CssR.compProxied.apply ),

        // Static jd-css styles:
        propsProxy.wrap(_ => jdCssStatic)( CssR.compProxied.apply ),

        // Dynamic jd-css styles (jd-runtime).
        s.jdCssC { CssR.compProxied.apply },

        // Cart Form Toolbar
        s.toolBarPropsC { itemsToolBarR.component.apply },

        MuiTable()(

          // Order items table header.
          s.orderHeadC { itemsTableHeadR.component.apply },

          // Order items table rows (tbody).
          s.orderBodyC { itemsTableBodyR.component.apply }

        ),

        <.br,
        <.br,

        // PAY button, when payment is possible (non-empty cart-order).
        s.goToPayPropsC { goToPayBtnR.component.apply },

        <.br,
        <.br,

        // Transactions list, if any:
        s.txnsPricedOptC { txnsR.component.apply }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        jdCssC = propsProxy.connect( _.order.jdRuntime.jdCss )( JdCss.JdCssFastEq ),

        orderHeadC = propsProxy.connect { props =>
          itemsTableHeadR.PropsVal(
            hasCheckedItems   = props.order.itemsSelected.nonEmpty,
            hasUnCheckedItems = props.order.orderContents.nonEmpty &&
              props.order.orderContents.exists { ocJs =>
                val items = ocJs.content.items
                items.nonEmpty &&
                (items.lengthCompare( props.order.itemsSelected.size ) > 0)
              },
            isPendingReq = props.order.orderContents.isPending,
            isItemsEditable = props.order.orderContents.exists(_.isItemsEditable)
          )
        }( itemsTableHeadR.ItemsTableHeadRPropsValFastEq ),

        orderBodyC = propsProxy.connect { props =>
          itemsTableBodyR.PropsVal(
            orderContents = props.order.orderContents,
            selectedIds   = props.order.itemsSelected,
            jdRuntime     = props.order.jdRuntime,
          )
        }( itemsTableBodyR.ItemsTableBodyRPropsValFastEq ),

        toolBarPropsC = propsProxy.connect { props =>
          // Toolbar is visible only on cart-order, not visible with payed/closed/etc orders.
          val ocPot = props.order.orderContents
          OptionUtil.maybe {
            ocPot.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
              oc.order.exists(_.status ==* MOrderStatuses.Draft)
            }
          } {
            itemsToolBarR.PropsVal(
              countSelected = props.order.itemsSelected.size,
              isPendingReq  = ocPot.isPending
            )
          }
        }( OptFastEq.Wrapped( itemsToolBarR.ItemsToolBarRPropsValFastEq ) ),

        goToPayPropsC = propsProxy.connect { props =>
          for {
            // TODO onNodeId: Allow to submit without nodeId, currenly Lk API historically depends on onNodeId, but not very needed.
            onNodeId <- props.conf.onNodeId
            if props.order.orderContents.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
              oc.order.exists(_.status ==* MOrderStatuses.Draft)
            }
          } yield {
            goToPayBtnR.PropsVal(
              onNodeId = onNodeId,
              disabled = props.order.orderContents.isPending
            )
          }
        }( OptFastEq.Wrapped( goToPayBtnR.GoToPayBtnRPropsValFastEq ) ),

        orderOptC = propsProxy.connect { mroot =>
          for {
            ocJs <- mroot.order.orderContents.toOption
            morder <- ocJs.content.order
            if morder.status !=* MOrderStatuses.Draft
          } yield {
            morder
          }
        }( OptFastEq.Plain ),

        txnsPricedOptC = propsProxy.connect { mroot =>
          for {
            ocJs <- mroot.order.orderContents.toOption
            oc = ocJs.content
            if oc.order.exists(_.status !=* MOrderStatuses.Draft)
          } yield {
            oc.txns
          }
        }( OptFastEq.Plain )

      )
    }
    .renderBackend[Backend]
    .build

}
