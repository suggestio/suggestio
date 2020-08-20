package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiListItem, MuiListItemProps, MuiListItemSecondaryAction, MuiListItemText, MuiListItemTextProps, MuiTable, MuiTableBody, MuiTableCell, MuiTableRow, MuiTableRowProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.bill.MPrice
import io.suggest.bill.tf.daily.MTfDailyInfo
import io.suggest.cal.m.MCalTypes
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{TfDailyEditClick, TfDailyShowDetails}
import io.suggest.msg.JsFormatUtil
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 20:23
  * Description: Компонент описания тарифа узла.
  */
final class TariffEditR(
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  case class PropsVal(
                       tfDailyOpt         : Option[MTfDailyInfo],
                       showExpanded       : Boolean,
                     )
  implicit val propsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.tfDailyOpt ===* b.tfDailyOpt) &&
    (a.showExpanded ==* b.showExpanded)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {

    /** Клик по ряду для развёртывания подробностей. */
    private lazy val _onRowClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TfDailyShowDetails )
    }

    private lazy val _onRowEditClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TfDailyEditClick )
    }

    def render(s: Props_t): VdomElement = {
      s.tfDailyOpt.whenDefinedEl { tfInfo =>
        crCtxP.consume { crCtx =>
          val perDay = crCtx.messages( MsgCodes.`_per_.day` )

          React.Fragment(
            MuiListItem(
              new MuiListItemProps {
                override val button     = !s.showExpanded
                override val onClick    = JsOptionUtil.maybeDefined( !s.showExpanded )( _onRowClickCbF )
              }
            )(
              // Текстовые данные.
              MuiListItemText {
                val prefixDiv = <.span(
                  crCtxP.message( tfInfo.mode.msgCode ),
                  HtmlConstants.COLON,
                )

                val _secondaryContent = if (s.showExpanded) {
                  // Раскрытый списочек.
                  prefixDiv(
                    <.br,
                    crCtxP.message( MsgCodes.`Comission.0.pct.for.sio`, tfInfo.comissionPct ),
                  )
                } else {
                  // Свёрнутый тариф, одной строкой
                  prefixDiv(
                    HtmlConstants.SPACE,
                    tfInfo.clauses.toVdomArray { case (mCalType, mPrice) =>
                      <.span(
                        ^.key := mCalType.value,
                        ReactCommonUtil.maybe( mCalType !=* tfInfo.clauses.head._1 ) {
                          TagMod(
                            HtmlConstants.SPACE,
                            HtmlConstants.SLASH,
                            HtmlConstants.SPACE,
                          )
                        },
                        ^.title := crCtx.messages( mCalType.i18nCode ),
                        MPrice.amountStr(mPrice)
                      )
                    },
                    <.span(
                      ^.title := crCtx.messages( tfInfo.currency.currencyNameI18n ),
                      crCtx.messages(tfInfo.currency.i18nPriceCode, "")
                    ),
                    perDay,
                    <.br,
                    // Подсказка показа подробностей по тарифу:
                    crCtx.messages( MsgCodes.`Show.details` ),
                    HtmlConstants.ELLIPSIS,
                  )
                }

                new MuiListItemTextProps {
                  override val primary = crCtxP.message( MsgCodes.`Adv.tariff` ).rawNode
                  override val secondary = _secondaryContent.rawElement
                }
              }(),

              // Кнопка редактирования тарифа:
              MuiListItemSecondaryAction()(
                MuiToolTip(
                  new MuiToolTipProps {
                    override val title = crCtx.messages( MsgCodes.`Change` ).rawNode
                  }
                )(
                  MuiIconButton(
                    new MuiIconButtonProps {
                      override val onClick = _onRowEditClickCbF
                    }
                  )(
                    Mui.SvgIcons.Edit()(),
                  )
                )
              ),
            ),

            ReactCommonUtil.maybeNode( s.showExpanded ) {
              MuiListItem()(
                MuiTable()(
                  MuiTableBody()(
                    // Сортировка по MCalType, чтобы будни всегда шли перед выходными.
                    (for {
                      mcal <- MCalTypes.values.iterator
                      tfInfo <- tfInfo.clauses.get( mcal )
                    } yield {
                      mcal -> tfInfo
                    })
                      .toVdomArray { case (mcal, mprice) =>
                        MuiTableRow.component.withKey( mcal.value )(
                          new MuiTableRowProps {}
                        )(
                          MuiTableCell()(
                            crCtx.messages( mcal.i18nCode )
                          ),
                          MuiTableCell()(
                            JsFormatUtil.formatPrice( mprice ),
                            perDay
                          ),
                        )
                      },
                  )
                ),
              )
            },
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( propsValFastEq ) )
    .build

}
