package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{MuiLinearProgress, MuiTableBody, MuiTableCell, MuiTableCellProps, MuiTableRow, MuiTableRowProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.bill.cart.{MOrderContent, MOrderItemRowOpts}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.JdCss
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 13:11
  * Description: Компонент наполнения списка-таблицы заказа.
  */
class ItemsTableBodyR(
                       val itemRowR           : ItemRowR,
                       val itemRowPreviewR    : ItemRowPreviewR
                     ) {

  /** Модель пропертисов компонента.
    *
    * @param items item'ы в ордере.
    * @param rcvrs Карта узлов для рендера названий ресиверов и остального.
    */
  case class PropsVal(
                       orderContents  : Pot[MOrderContent],
                       selectedIds    : Set[Gid_t],
                       rowOpts        : MOrderItemRowOpts,
                       jdCss          : JdCss,
                     )
  implicit object ItemsTableBodyRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.orderContents  ===* b.orderContents) &&
        (a.selectedIds  ===* b.selectedIds) &&
        (a.rowOpts      ===* b.rowOpts) &&
        (a.jdCss        ===* b.jdCss)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  /** Тело компонента, занимающегося рендером тела списка item'ов. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      // Сколько колонок всего? Не всегда это важно.
      lazy val columnsCount = 3 + props.rowOpts.toAddColsCount

      def fullRowCell(cells: VdomNode*) =
        MuiTableRow()(
          MuiTableCell(
            new MuiTableCellProps {
              val colSpan = columnsCount
            }
          )(cells: _*)
        )

      MuiTableBody()(

        // TODO renderEmpty - что рендерить? По идее, это не нужно, т.к. никогда не должно быть видимо Pot.empty.

        // Рендерить ряды, навешивая key на каждый.
        props.orderContents.render { orderContent =>
          if ( orderContent.items.isEmpty ) {
            // Пустая корзина.
            fullRowCell(
              Messages( MsgCodes.`Your.cart.is.empty` )
            )

          } else {
            // Есть что рендерить: рендерим элементы.
            var iter = for {
              // Проходим группы item'ов в рамках каждой карточки:
              (nodeId, nodeItems) <- orderContent.adId2itemsMap.iterator
              nodeItemsCount = nodeItems.length

              // Собрать ячейку с jd-рендером.
              preview = propsProxy.wrap { _ =>
                for {
                  jdAdData <- orderContent.adId2jdDataMap.get( nodeId )
                } yield {
                  itemRowPreviewR.PropsVal(
                    jdArgs    = MJdArgs(
                      template = jdAdData.template,
                      edges = jdAdData.edgesMap
                        // TODO Инстанс карты с js-эджами следует собирать не тут, а в контроллере и хранить в состоянии.
                        .mapValues(MEdgeDataJs(_)),
                      jdCss = props.jdCss,
                      conf  = props.jdCss.jdCssArgs.conf
                    ),
                    jdRowSpan = nodeItemsCount
                  )
                }
              }( itemRowPreviewR.apply )

              // Пройти item'ы, наконец:
              (mitem, i) <- nodeItems.iterator.zipWithIndex

            } yield {
              propsProxy.wrap { _ =>
                itemRowR.PropsVal(
                  mitem         = mitem,
                  rowOpts       = props.rowOpts,
                  isSelected    = mitem.id.map(props.selectedIds.contains),
                  rcvrNode      = mitem.rcvrIdOpt.flatMap( orderContent.rcvrsMap.get ),
                  isPendingReq  = props.orderContents.isPending
                )
              } { itemPropsValProxy =>
                itemRowR.component
                  .withKey(nodeId + i)( itemPropsValProxy )(
                    // Рендерить первую ячейку с превьюшкой надо только в первом ряду, остальные ряды - rowspan'ятся
                    ReactCommonUtil.maybeNode( i ==* 0 )( preview )
                  )
              }
            }
            val resAcc = iter.toVdomArray

            // Если заданы order-prices, то надо ещё отрендерить ряд ИТОГО
            if (orderContent.orderPrices.nonEmpty) {
              val emptyCell = MuiTableCell()()
              val typoProps = new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.subheading
              }
              resAcc ++= List(
                // spacer
                MuiTableRow.component.withKey("s")( MuiTableRowProps.empty )(
                  MuiTableCell(
                    new MuiTableCellProps {
                      val colSpan = columnsCount
                    }
                  )()
                ),
                // Итого-ряд:
                MuiTableRow.component.withKey("t")(MuiTableRowProps.empty)(
                  emptyCell,
                  MuiTableCell()(
                    MuiTypoGraphy( typoProps )(
                      Messages( MsgCodes.`Total` ),
                      HtmlConstants.SPACE,
                      HtmlConstants.`(`, orderContent.items.length, HtmlConstants.`)`,
                      HtmlConstants.COLON
                    )
                  ),
                  MuiTableCell()(
                    orderContent.orderPrices.toVdomArray { mprice =>
                      MuiTypoGraphy.component.withKey(mprice.currency.value)( typoProps )(
                        JsFormatUtil.formatPrice(mprice)
                      )
                    }
                  )
                )
              )
            }
            resAcc
          }

        },

        // Если идёт запрос, то рендерить "ожидалку".
        props.orderContents.renderPending { _ =>
          fullRowCell(
            MuiLinearProgress()
          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
