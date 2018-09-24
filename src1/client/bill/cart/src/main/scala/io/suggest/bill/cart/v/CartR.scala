package io.suggest.bill.cart.v

import chandu0101.scalajs.react.components.materialui.MuiTable
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.order.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:20
  * Description: Корневой компонент корзины приобретённых товаров и услуг.
  */
class CartR(
             val itemRowR           : ItemRowR,
             val itemsTableBodyR    : ItemsTableBodyR,
             val itemsTableHeadR    : ItemsTableHeadR,
           ) {

  import itemsTableHeadR.ItemsTableHeadRPropsValFastEq
  import itemsTableBodyR.ItemsTableBodyRPropsValFastEq


  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    orderHeadC   : ReactConnectProxy[itemsTableHeadR.Props_t],
                    orderBodyC   : ReactConnectProxy[itemsTableBodyR.Props_t]
                  )

  /** Ядро компонента корзины. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(
        MuiTable()(

          // Заголовок таблицы ордеров.
          s.orderHeadC { itemsTableHeadR.apply },

          // Содержимое таблицы.
          s.orderBodyC { itemsTableBodyR.apply }

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        orderHeadC = propsProxy.connect { props =>
          itemsTableHeadR.PropsVal(
            hasCheckedItems   = props.data.itemsSelected.nonEmpty,
            hasUnCheckedItems = props.data.orderContents.nonEmpty &&
              props.data.orderContents.exists { oc =>
                oc.items.nonEmpty &&
                  (oc.items.lengthCompare( props.data.itemsSelected.size ) > 0)
              },
            rowOpts = props.conf.orderRowOpts
          )
        },

        orderBodyC = propsProxy.connect { props =>
          itemsTableBodyR.PropsVal(
            orderContents = props.data.orderContents,
            selectedIds   = props.data.itemsSelected,
            rowOpts       = props.conf.orderRowOpts,
            jdCss         = props.data.jdCss
          )
        }

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
