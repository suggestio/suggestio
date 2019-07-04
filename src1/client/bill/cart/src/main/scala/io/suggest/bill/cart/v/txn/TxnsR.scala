package io.suggest.bill.cart.v.txn

import com.materialui
import com.materialui.{MuiTable, MuiTableBody, MuiTableCell, MuiTableCellProps, MuiTableHead, MuiTableRow, MuiTableRowProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.txn.MTxnPriced
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.sjs.dom.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.18 15:46
  * Description: React-компонент для рендера списка транзакций.
  */
class TxnsR {

  type Props_t = Option[Seq[MTxnPriced]]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsOptProxy: Props): VdomElement = {

      val tzOffset = CommonDateTimeUtil.minutesOffset2TzOff( DomQuick.tzOffsetMinutes )

      propsOptProxy.value.whenDefinedEl { mtxns =>
        <.div(

          // Заголовок:
          MuiTypoGraphy(
            new MuiTypoGraphyProps {
              override val variant = MuiTypoGraphyVariants.h5
            }
          )(
            Messages( MsgCodes.`Transactions` )
          ),

          MuiTable()(

            // Заголовок таблицы
            MuiTableHead()(
              MuiTableRow()(

                MuiTableCell()(
                  Messages( MsgCodes.`Bill.id` )
                ),

                MuiTableCell()(
                  Messages( MsgCodes.`Bill.details` )
                ),

                MuiTableCell()(
                  Messages( MsgCodes.`Date` )
                ),

                MuiTableCell()(
                  Messages( MsgCodes.`Sum` )
                )

              )
            ),


            // Наполнение таблицы транзаций:
            MuiTableBody()(

              // Нет транзакций.
              ReactCommonUtil.maybeNode( mtxns.isEmpty ) {
                MuiTableRow()(
                  materialui.MuiTableCell(
                    new MuiTableCellProps {
                      val colSpan = 4
                    }
                  )(
                    Messages( MsgCodes.`No.transactions.found` )
                  )
                )
              },

              // Есть транзакции
              mtxns.toVdomArray { mtxnPriced =>
                val mtxn = mtxnPriced.txn
                MuiTableRow.component
                  .withKey( mtxn.id.fold(mtxn.toString)(_.toString) )( MuiTableRowProps.empty )(

                    MuiTableCell()(
                      mtxn.id.whenDefinedNode(_.toString)
                    ),

                    MuiTableCell()(
                      mtxn.orderIdOpt.whenDefinedNode { orderId =>
                        Messages( MsgCodes.`Payment.for.order.N`, orderId )
                      },
                      HtmlConstants.SPACE,
                      mtxn.paymentComment
                        .whenDefinedNode(identity(_))
                    ),

                    {
                      val dtOff = mtxn.dateProcessed.withOffsetSameInstant( tzOffset )
                      MuiTableCell()(
                        YmdR(
                          dtOff.toLocalDate.toYmd
                        )(),
                        // Рендер времени, пока всырую. TZ неявно вкалькулирована в текущее время браузера.
                        {
                          val time = dtOff.toLocalTime
                          // Можно приплюсовать сдвиг из js.Date, и будет локальное время.
                          VdomArray(
                            time.getHour,
                            HtmlConstants.COLON,
                            time.getMinute,
                            HtmlConstants.COLON,
                            time.getSecond
                          )
                        }
                      )
                    },

                    MuiTableCell()(
                      JsFormatUtil.formatPrice( mtxnPriced.price )
                    )

                  )
              }

            )

          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
