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
  * Description: Корневой компонент корзины приобретённых товаров и услуг.
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

  /** Ядро компонента корзины. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Краткое описание заказа, если требуется:
        s.orderOptC { orderInfoR.apply },

        // Статические стили плитки.
        propsProxy.wrap(_ => cartCss)( CssR.apply ),

        // Статические jd-стили:
        propsProxy.wrap(_ => jdCssStatic)( CssR.apply ),

        // Рендер стилей отображаемых карточек.
        s.jdCssC { CssR.apply },

        // Панелька-тулбар
        s.toolBarPropsC { itemsToolBarR.apply },

        MuiTable()(

          // Заголовок таблицы ордеров.
          s.orderHeadC { itemsTableHeadR.apply },

          // Содержимое таблицы.
          s.orderBodyC { itemsTableBodyR.apply }

        ),

        <.br,
        <.br,

        // Кнопка перехода к оплате, когда оплата возможна (ордер-корзина + есть item'ы).
        s.goToPayPropsC { goToPayBtnR.apply },

        <.br,
        <.br,

        // Список транзакций, если есть:
        s.txnsPricedOptC { txnsR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        jdCssC = propsProxy.connect( _.order.jdCss )( JdCss.JdCssFastEq ),

        orderHeadC = propsProxy.connect { props =>
          itemsTableHeadR.PropsVal(
            hasCheckedItems   = props.order.itemsSelected.nonEmpty,
            hasUnCheckedItems = props.order.orderContents.nonEmpty &&
              props.order.orderContents.exists { oc =>
                oc.items.nonEmpty &&
                  (oc.items.lengthCompare( props.order.itemsSelected.size ) > 0)
              },
            isPendingReq = props.order.orderContents.isPending,
            isItemsEditable = props.order.orderContents.exists(_.isItemsEditable)
          )
        }( itemsTableHeadR.ItemsTableHeadRPropsValFastEq ),

        orderBodyC = propsProxy.connect { props =>
          itemsTableBodyR.PropsVal(
            orderContents = props.order.orderContents,
            selectedIds   = props.order.itemsSelected,
            jdCss         = props.order.jdCss
          )
        }( itemsTableBodyR.ItemsTableBodyRPropsValFastEq ),

        toolBarPropsC = propsProxy.connect { props =>
          // Нужно отображать тулбар только если ордер-корзина
          val ocPot = props.order.orderContents
          OptionUtil.maybe {
            ocPot.exists { oc =>
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
          // TODO Разрешить сабмит без nodeId. Зависимость от nodeId - на уровне ЛК, хотя можно и без неё.
          for {
            onNodeId <- props.conf.onNodeId
            if props.order.orderContents.exists { ord =>
              ord.items.nonEmpty &&
                ord.order.exists(_.status ==* MOrderStatuses.Draft)
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
            oc <- mroot.order.orderContents.toOption
            morder <- oc.order
            if morder.status !=* MOrderStatuses.Draft
          } yield {
            morder
          }
        }( OptFastEq.Plain ),

        txnsPricedOptC = propsProxy.connect { mroot =>
          for {
            oc <- mroot.order.orderContents.toOption
            if oc.order.exists(_.status !=* MOrderStatuses.Draft)
          } yield {
            oc.txns
          }
        }( OptFastEq.Plain )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
