package io.suggest.bill.cart.v.txn

import com.materialui
import com.materialui.{MuiPropsBaseStatic, MuiTable, MuiTableBody, MuiTableCell, MuiTableCellProps, MuiTableHead, MuiTableRow, MuiTableRowProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.mbill2.m.txn.MTxnPriced
import io.suggest.msg.JsFormatUtil
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._

import java.time.format.DateTimeFormatter

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.18 15:46
  * Description: Transactions list react-component.
  */
class TxnsR(
             crCtxP                    : React.Context[MCommonReactCtx],
           ) {

  type Props_t = Option[Seq[MTxnPriced]]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsOptProxy: Props): VdomElement = {

      val tzOffset = CommonDateTimeUtil.minutesOffset2TzOff( DomQuick.tzOffsetMinutes )

      propsOptProxy.value.whenDefinedEl { mtxns =>
        crCtxP.consume { crCtx =>
        <.div(

          // Header:
          MuiTypoGraphy(
            new MuiTypoGraphyProps {
              override val variant = MuiTypoGraphyVariants.h5
            }
          )(
            crCtx.messages( MsgCodes.`Transactions` ),
          ),

          MuiTable()(

            // Table header
            MuiTableHead()(
              MuiTableRow()(

                MuiTableCell()(
                  crCtx.messages( MsgCodes.`Bill.id` )
                ),

                MuiTableCell()(
                  crCtx.messages( MsgCodes.`Bill.details` )
                ),

                MuiTableCell()(
                  crCtx.messages( MsgCodes.`Date` )
                ),

                MuiTableCell()(
                  crCtx.messages( MsgCodes.`Sum` )
                )

              )
            ),


            // Transactions list body:
            MuiTableBody()(

              // If no transactions...
              ReactCommonUtil.maybeNode( mtxns.isEmpty ) {
                MuiTableRow()(
                  materialui.MuiTableCell(
                    new MuiTableCellProps {
                      val colSpan = 4
                    }
                  )(
                    crCtx.messages( MsgCodes.`No.transactions.found` )
                  )
                )
              },

              // Have at least one transaction:
              mtxns.toVdomArray { mtxnPriced =>
                val mtxn = mtxnPriced.txn
                MuiTableRow.component
                  .withKey( mtxn.id.fold(mtxn.toString)(_.toString) )( MuiPropsBaseStatic.empty[MuiTableRowProps] )(

                    MuiTableCell()(
                      mtxn.id.whenDefinedNode(_.toString)
                    ),

                    MuiTableCell()(
                      crCtx.messages( mtxn.txType.i18nCode ),
                      mtxn.paymentComment.whenDefinedNode { pComment =>
                        VdomArray(
                          <.br,
                          pComment,
                        )
                      }
                    ),

                    {
                      val dtOff = mtxn.datePaid
                        .getOrElse( mtxn.dateProcessed )
                        .withOffsetSameInstant( tzOffset )

                      MuiTableCell()(
                        YmdR(
                          dtOff.toLocalDate.toYmd
                        )(),
                        " - ",
                        // Render time, raw time by now. Timezone implicitly "inlined" into browser's current time.
                        // It is possible to increment offset from js.Date, and local time will be here.
                        DateTimeFormatter.ISO_LOCAL_TIME.format( dtOff.toLocalTime.withNano(0) )
                      )
                    },

                    MuiTableCell()(
                      JsFormatUtil.formatPrice( mtxnPriced.price )
                    ),

                  )
              }

            )

          )
        )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
