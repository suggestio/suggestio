package io.suggest.bill.cart.v

import chandu0101.scalajs.react.components.materialui.{MuiTable, MuiTableBody, MuiTableCell, MuiTableHead, MuiTableRow}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.mbill2.m.item.MItem
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:46
  * Description: Компонент для рендера содержимого заказа.
  */
class OrderContentsR {

  case class PropsVal(
                       items: Seq[MItem]
                     )
  object OrderContentsRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.items ===* b.items
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsProxy: Props): VdomElement = {
      MuiTable()(
        MuiTableHead()(
          MuiTableRow()(
           // TODO Заголовок. Зависим от режимов работы: mutable/immutable.
           MuiTableCell()(
             "col1"
           ),
           MuiTableCell()(
             "col2"
           )
          )
        ),

        MuiTableBody()(
          // TODO Рендер списка item'ов сюда
        )
      )
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
