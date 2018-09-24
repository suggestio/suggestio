package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{MuiCheckBox, MuiCheckBoxProps, MuiTableCell, MuiTableHead, MuiTableRow}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.MOrderItemRowOpts
import io.suggest.bill.cart.m.CartSelectItem
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:46
  * Description: Компонент для рендера шапки таблицы заказа.
  */
class ItemsTableHeadR {

  /** Модель пропертисов элемента. */
  case class PropsVal(
                       hasCheckedItems      : Boolean,
                       hasUnCheckedItems    : Boolean,
                       rowOpts              : MOrderItemRowOpts
                     )
  implicit object ItemsTableHeadRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.hasCheckedItems       ==* b.hasCheckedItems) &&
      (a.hasUnCheckedItems     ==* b.hasUnCheckedItems) &&
      (a.rowOpts              ===* b.rowOpts)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend( $: BackendScope[Props, Unit] ) {

    private def _onCheckBoxClick(e: ReactEventFromInput): Callback = {
      val checked = e.target.checked
      dispatchOnProxyScopeCB($, CartSelectItem(itemId = None, checked = checked))
    }
    private lazy val _onCheckBoxClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onCheckBoxClick )


    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      // Заголовок таблицы.
      MuiTableHead()(
        MuiTableRow()(

          // Столбец рендера карточки
          MuiTableCell()(
            HtmlConstants.NBSP_STR
          ),

          // Столбец названия товара
          MuiTableCell()(
            Messages( MsgCodes.`_order.Items` )
          ),

          // Столбец стоимости товара
          MuiTableCell()(
            Messages( MsgCodes.`Price` )
          ),

          // Опциональный столбец статуса обработки одного item'а.
          ReactCommonUtil.maybeNode( props.rowOpts.withStatus ) {
            MuiTableCell()(
              HtmlConstants.NBSP_STR
            )
          },

          // Разрешён рендер галочки?
          ReactCommonUtil.maybeNode( props.rowOpts.withCheckBox ) {
            MuiTableCell()(

              // Есть хотя бы один какой-либо item? Иначе в галочке нет смысла.
              if (props.hasUnCheckedItems || props.hasCheckedItems) {
                MuiCheckBox(
                  new MuiCheckBoxProps {
                    override val onChange = _onCheckBoxClickCbF
                    override val checked = js.defined {
                      props.hasCheckedItems && !props.hasUnCheckedItems
                    }
                    override val indeterminate = props.hasCheckedItems && props.hasUnCheckedItems
                  }
                )
              } else {
                HtmlConstants.NBSP_STR
              }

            )
          }

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
