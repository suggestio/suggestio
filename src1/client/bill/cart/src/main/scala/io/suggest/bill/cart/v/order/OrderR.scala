package io.suggest.bill.cart.v.order

import com.materialui.{MuiCollapse, MuiTable, MuiToolBar, MuiToolBarClasses, MuiToolBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.itm.{ItemsDeleteSelectedR, ItemsTableBodyR, ItemsTableHeadR}
import io.suggest.bill.cart.v.pay.{CartPayR, PayButtonR}
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
              orderCss               : OrderCss,
              jdCssStatic            : JdCssStatic,
              cartPayR               : CartPayR,
              val itemsTableHeadR    : ItemsTableHeadR,
              val itemsTableBodyR    : ItemsTableBodyR,
              val itemsDeleteSelectedR  : ItemsDeleteSelectedR,
              val orderInfoR         : OrderInfoR,
              val txnsR              : TxnsR,
              val payButtonR         : PayButtonR,
              unholdOrderDiaR        : UnholdOrderDiaR,
            ) {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    jdCssC          : ReactConnectProxy[JdCss],
                    orderHeadC      : ReactConnectProxy[itemsTableHeadR.Props_t],
                    orderBodyC      : ReactConnectProxy[itemsTableBodyR.Props_t],
                    deleteSelectedC : ReactConnectProxy[itemsDeleteSelectedR.Props_t],
                    orderOptC       : ReactConnectProxy[orderInfoR.Props_t],
                    txnsPricedOptC  : ReactConnectProxy[txnsR.Props_t],
                    payButtonOptC   : ReactConnectProxy[payButtonR.Props_t],
                    isOrderVisibleSomeC: ReactConnectProxy[Some[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      val collapsable = List[VdomElement](
        propsProxy.wrap(_.order.unHoldOrder)( unholdOrderDiaR.component.apply ),

        // Short order description, if any.
        s.orderOptC { orderInfoR.component.apply },

        // Static cart form CSS styles:
        CssR.component( orderCss ),

        // Static jd-css styles:
        propsProxy.wrap(_ => jdCssStatic)( CssR.compProxied.apply ),

        // Dynamic jd-css styles (jd-runtime).
        s.jdCssC { CssR.compProxied.apply },

        MuiTable()(

          // Order items table header.
          s.orderHeadC { itemsTableHeadR.component.apply },

          // Order items table rows (tbody).
          s.orderBodyC { itemsTableBodyR.component.apply },

        ),

        MuiToolBar(
          new MuiToolBarProps {
            override val classes = new MuiToolBarClasses {
              override val root = orderCss.ItemsTable.ToolBar.root.htmlClass
            }
            override val disableGutters = true
          }
        )(
          // Cart Form Toolbar
          s.deleteSelectedC { itemsDeleteSelectedR.component.apply },

          // PAY button, when payment is possible (non-empty cart-order).
          s.payButtonOptC { payButtonR.component.apply },
        ),

        // Transactions list, if any:
        s.txnsPricedOptC { txnsR.component.apply },

      )

      <.div(

        s.isOrderVisibleSomeC { isVisibleSomeProxy =>
          MuiCollapse.component(
            new MuiCollapse.Props {
              override val in = isVisibleSomeProxy.value.value
              override val orientation = MuiCollapse.Orientation.VERTICAL
            }
          )( collapsable: _* )
        },

        // Payment controls:
        cartPayR.component( propsProxy ),

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

        deleteSelectedC = propsProxy.connect { props =>
          // Toolbar is visible only on cart-order, not visible with payed/closed/etc orders.
          val ocPot = props.order.orderContents
          OptionUtil.maybe {
            ocPot.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
              oc.order.exists(_.status ==* MOrderStatuses.Draft)
            }
          } {
            itemsDeleteSelectedR.PropsVal(
              countSelected = props.order.itemsSelected.size,
              isPendingReq  = ocPot.isPending
            )
          }
        }( OptFastEq.Wrapped( itemsDeleteSelectedR.ItemsToolBarRPropsValFastEq ) ),

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
        }( OptFastEq.Plain ),

        payButtonOptC = propsProxy.connect { props =>
          OptionUtil.maybeOpt(
            props.order.orderContents.exists { ocJs =>
              val oc = ocJs.content
              oc.items.nonEmpty &&
                oc.order.exists(_.status ==* MOrderStatuses.Draft)
            } && {
              val psIni = props.pay.paySystemInit
              psIni.isEmpty || psIni.isFailed
            } && {
              props.order.itemsSelected.isEmpty
            }
          ) (
            OptionUtil.SomeBool {
              !props.order.orderContents.isPending &&
              !props.pay.cartSubmit.isPending &&
              !props.pay.paySystemInit.isPending
            }
          )
        },

        isOrderVisibleSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool {
            !props.pay.paySystemInit.isReady
          }
        },

      )
    }
    .renderBackend[Backend]
    .build

}
