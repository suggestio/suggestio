package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.MuiTable
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.itm.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR, ItemsToolBarR}
import io.suggest.css.CssR
import io.suggest.jd.render.v.{JdCss, JdCssR}
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.spa.OptFastEq.Wrapped
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
              jdCssR                 : JdCssR,
              val itemRowR           : ItemRowR,
              val itemsTableBodyR    : ItemsTableBodyR,
              val itemsTableHeadR    : ItemsTableHeadR,
              val itemsToolBarR      : ItemsToolBarR,
              val goToPayBtnR        : GoToPayBtnR,
              val orderInfoR         : OrderInfoR,
            ) {

  import JdCss.JdCssFastEq
  import goToPayBtnR.GoToPayBtnRPropsValFastEq
  import itemsTableBodyR.ItemsTableBodyRPropsValFastEq
  import itemsTableHeadR.ItemsTableHeadRPropsValFastEq
  import itemsToolBarR.ItemsToolBarRPropsValFastEq


  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    jdCssC          : ReactConnectProxy[JdCss],
                    orderHeadC      : ReactConnectProxy[itemsTableHeadR.Props_t],
                    orderBodyC      : ReactConnectProxy[itemsTableBodyR.Props_t],
                    toolBarPropsC   : ReactConnectProxy[itemsToolBarR.Props_t],
                    goToPayPropsC   : ReactConnectProxy[goToPayBtnR.Props_t],
                    orderOptC       : ReactConnectProxy[orderInfoR.Props_t],
                  )

  /** Ядро компонента корзины. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Краткое описание заказа, если требуется:
        s.orderOptC { orderInfoR.apply },

        // Статические стили плитки.
        propsProxy.wrap(_ => cartCss)( CssR.apply ),

        // Рендер стилей отображаемых карточек.
        s.jdCssC { jdCssR.apply },

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
        s.goToPayPropsC { goToPayBtnR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        jdCssC = propsProxy.connect( _.order.jdCss )(JdCssFastEq),

        orderHeadC = propsProxy.connect { props =>
          itemsTableHeadR.PropsVal(
            hasCheckedItems   = props.order.itemsSelected.nonEmpty,
            hasUnCheckedItems = props.order.orderContents.nonEmpty &&
              props.order.orderContents.exists { oc =>
                oc.items.nonEmpty &&
                  (oc.items.lengthCompare( props.order.itemsSelected.size ) > 0)
              },
            rowOpts = props.conf.orderRowOpts,
            isPendingReq = props.order.orderContents.isPending
          )
        },

        orderBodyC = propsProxy.connect { props =>
          itemsTableBodyR.PropsVal(
            orderContents = props.order.orderContents,
            selectedIds   = props.order.itemsSelected,
            rowOpts       = props.conf.orderRowOpts,
            jdCss         = props.order.jdCss
          )
        },

        toolBarPropsC = propsProxy.connect { props =>
          itemsToolBarR.PropsVal(
            countSelected = props.order.itemsSelected.size,
            isPendingReq  = props.order.orderContents.isPending
          )
        },

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
        },

        orderOptC = propsProxy.connect { mroot =>
          for {
            oc <- mroot.order.orderContents.toOption
            morder <- oc.order
            if morder.status !=* MOrderStatuses.Draft
          } yield {
            morder
          }
        }

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
