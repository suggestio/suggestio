package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{MuiTableBody, MuiTableCell, MuiTableRow}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.m.order.MOrderItemRowOpts
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 13:11
  * Description: Компонент наполнения списка-таблицы заказа.
  */
class ItemsTableBodyR {

  /** Модель пропертисов компонента.
    *
    * @param items item'ы в ордере.
    * @param rcvrs Карта узлов для рендера названий ресиверов и остального.
    */
  case class PropsVal(
                       items          : Seq[MItem],
                       selectedIds    : Set[Gid_t],
                       rcvrs          : Map[String, MAdvGeoMapNodeProps],
                       rowOpts        : MOrderItemRowOpts
                     )
  implicit object ItemsTableBodyRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.items          ===* b.items) &&
        (a.selectedIds  ===* b.selectedIds) &&
        (a.rcvrs        ===* b.rcvrs) &&
        (a.rowOpts      ===* b.rowOpts)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  /** Тело компонента, занимающегося рендером тела списка item'ов. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      MuiTableBody()(
        // TODO Рендерить ряды, навешивая key на каждый.

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
