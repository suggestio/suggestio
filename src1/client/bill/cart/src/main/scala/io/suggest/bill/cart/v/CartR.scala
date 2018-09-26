package io.suggest.bill.cart.v

import chandu0101.scalajs.react.components.materialui.MuiTable
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.MCartRootS
import io.suggest.bill.cart.v.order.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR, ItemsToolBarR}
import io.suggest.css.CssR
import io.suggest.jd.render.v.{JdCss, JdCssR}
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
             cartCss                : CartCss,
             jdCssR                 : JdCssR,
             val itemRowR           : ItemRowR,
             val itemsTableBodyR    : ItemsTableBodyR,
             val itemsTableHeadR    : ItemsTableHeadR,
             val itemsToolBarR      : ItemsToolBarR,
           ) {

  import itemsTableHeadR.ItemsTableHeadRPropsValFastEq
  import itemsTableBodyR.ItemsTableBodyRPropsValFastEq
  import JdCss.JdCssFastEq
  import itemsToolBarR.ItemsToolBarRPropsValFastEq


  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    jdCssC          : ReactConnectProxy[JdCss],
                    orderHeadC      : ReactConnectProxy[itemsTableHeadR.Props_t],
                    orderBodyC      : ReactConnectProxy[itemsTableBodyR.Props_t],
                    toolBarPropsC   : ReactConnectProxy[itemsToolBarR.Props_t],
                  )

  /** Ядро компонента корзины. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

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

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        jdCssC = propsProxy.connect( _.data.jdCss )(JdCssFastEq),

        orderHeadC = propsProxy.connect { props =>
          itemsTableHeadR.PropsVal(
            hasCheckedItems   = props.data.itemsSelected.nonEmpty,
            hasUnCheckedItems = props.data.orderContents.nonEmpty &&
              props.data.orderContents.exists { oc =>
                oc.items.nonEmpty &&
                  (oc.items.lengthCompare( props.data.itemsSelected.size ) > 0)
              },
            rowOpts = props.conf.orderRowOpts,
            isPendingReq = props.data.orderContents.isPending
          )
        },

        orderBodyC = propsProxy.connect { props =>
          itemsTableBodyR.PropsVal(
            orderContents = props.data.orderContents,
            selectedIds   = props.data.itemsSelected,
            rowOpts       = props.conf.orderRowOpts,
            jdCss         = props.data.jdCss
          )
        },

        toolBarPropsC = propsProxy.connect { props =>
          itemsToolBarR.PropsVal(
            countSelected = props.data.itemsSelected.size,
            isPendingReq  = props.data.orderContents.isPending
          )
        }

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
