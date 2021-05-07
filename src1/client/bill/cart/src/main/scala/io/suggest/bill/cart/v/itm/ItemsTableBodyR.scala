package io.suggest.bill.cart.v.itm

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiPropsBaseStatic, MuiTableBody, MuiTableCell, MuiTableCellClasses, MuiTableCellProps, MuiTableRow, MuiTableRowProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.bill.cart.m.{LoadCurrentOrder, MOrderContentJs}
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.{MJdArgs, MJdRuntime}
import io.suggest.jd.render.v.JdR
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactDiodeUtil.Implicits._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 13:11
  * Description: Order items table display component.
  */
class ItemsTableBodyR(
                       orderCss               : OrderCss,
                       jdR                    : JdR,
                       val itemRowR           : ItemRowR,
                     ) {

  case class PropsVal(
                       orderContents  : Pot[MOrderContentJs],
                       selectedIds    : Set[Gid_t],
                       jdRuntime      : MJdRuntime,
                     )
  implicit object ItemsTableBodyRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.orderContents      ===* b.orderContents) &&
      (a.selectedIds        ===* b.selectedIds) &&
      (a.jdRuntime          ===* b.jdRuntime)
    }
  }


  /** How many columns total? Not always it make sense. */
  private final def TOTAL_COLUMNS_COUNT = 4

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    lazy val _onRefreshBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb { (e: ReactEvent) =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, LoadCurrentOrder)
    }


    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      // fullRowCell properties
      lazy val fullRowCellProps = {
        val css = new MuiTableCellClasses {
          override val root = orderCss.ItemsTable.TBody.fullRowCell.htmlClass
        }
        new MuiTableCellProps {
          override val classes = css
          val colSpan = TOTAL_COLUMNS_COUNT
        }
      }

      def fullRowCell(cells: VdomNode*) =
        MuiTableRow()(
          MuiTableCell(
            fullRowCellProps
          )(cells: _*)
        )

      MuiTableBody()(

        // TODO renderEmpty - what to render? Well, theoretically, it is not needed, because Pot.empty must NOT be here.

        // Render errors for items list. And retry/reload button.
        props.orderContents.renderFailed { ex =>
          fullRowCell(
            Mui.SvgIcons.Warning()(),
            MuiToolTip(
              new MuiToolTipProps {
                override val title: React.Node = ex.toString
              }
            )(
              MuiTypoGraphy(
                new MuiTypoGraphyProps {
                  override val noWrap = true
                  override val variant = MuiTypoGraphyVariants.caption
                }
              )(
                Messages( MsgCodes.`Something.gone.wrong` ),
                HtmlConstants.ELLIPSIS
              )
            ),

            // Reload button
            MuiToolTip(
              new MuiToolTipProps {
                override val title: React.Node = Messages( MsgCodes.`Reload` )
              }
            )(
              MuiIconButton(
                new MuiIconButtonProps {
                  override val onClick = _onRefreshBtnClickJsCbF
                }
              )(
                Mui.SvgIcons.Refresh()()
              )
            )
          )
        },

        // Render keyed rows:
        props.orderContents.render { orderContentJs =>
          val orderContent = orderContentJs.content
          if ( orderContent.items.isEmpty ) {
            // Cart is empty:
            fullRowCell(
              Messages( MsgCodes.`Your.cart.is.empty` )
            )

          } else {
            // Cart non-empty. Render items table:
            var iter = for {
              // For each ad, walk item groups (group per ad):
              (nodeId, nodeItems) <- orderContentJs.adId2itemsMap.iterator
              nodeItemsCount = nodeItems.length

              // Cell with jd-rendered ad or something visually related:
              previewOpt = {
                // Try to render ad:
                orderContentJs
                  .adId2jdDataMap
                  .get( nodeId )
                  .map[VdomElement] { jdAdDataJs =>
                    val jdArgs = MJdArgs(
                      data      = jdAdDataJs,
                      jdRuntime = props.jdRuntime,
                      conf      = props.jdRuntime.jdCss.jdCssArgs.conf,
                    )
                    val propsProxy2 = propsProxy.resetZoom( jdArgs )
                    jdR( propsProxy2 )
                  }
                  .orElse {
                    // No ad. Looking for ADN Node logo:
                    orderContentJs
                      .adnNodesMap
                      .get( nodeId )
                      .map[VdomElement] { nodeProps =>
                        // Render visible part: icon or something else.
                        val nodeName = nodeProps.nameOrIdOrEmpty
                        MuiToolTip {
                          new MuiToolTipProps {
                            override val title: React.Node = nodeName
                          }
                        } {
                          nodeProps.logoOpt
                            .map[VdomElement] { icon =>
                              <.img(
                                orderCss.ItemsTable.NodePreviewColumn.adnLogo,
                                ^.src := icon.url,
                                // Bg color need for logo, but not needed for wcAsLogo, TODO Resolve bg color? Need to resolve?
                                //nodeProps.colors.bg.whenDefined { mcd =>
                                //  ^.backgroundColor := mcd.hexCode
                                //}
                              )
                            }
                            .getOrElse[VdomElement] {
                              // No ad, no logo. Just render node name:
                              MuiTypoGraphy(
                                new MuiTypoGraphyProps {
                                  override val variant = MuiTypoGraphyVariants.subtitle1
                                }
                              )( nodeName )
                            }
                        }
                      }
                  }
              }

              // Walk all node items:
              (mitem, i) <- nodeItems.iterator.zipWithIndex

            } yield {
              val isWithPreview = (i ==* 0)
              propsProxy.wrap { _ =>
                itemRowR.PropsVal(
                  mitem           = mitem,
                  isItemEditable  = orderContentJs.isItemsEditable,
                  isSelected      = mitem.id.map(props.selectedIds.contains),
                  rcvrNode        = mitem.rcvrIdOpt.flatMap( orderContentJs.adnNodesMap.get ),
                  isPendingReq    = props.orderContents.isPending,
                  previewRowSpan  = OptionUtil.maybe(isWithPreview)(nodeItemsCount)
                )
              } { itemPropsValProxy =>
                itemRowR.component
                  .withKey(nodeId + HtmlConstants.UNDERSCORE + mitem.id.get)( itemPropsValProxy )(
                    // Render first cell with preview only in first row. Other rows in column are ROWSPANned.
                    ReactCommonUtil.maybeNode( isWithPreview )( previewOpt )
                  )
              }
            }
            val resAcc = iter.toVdomArray

            // If order-prices is defined, when render PRICE TOTAL.
            if (orderContent.orderPrices.nonEmpty) {
              val emptyCell = MuiTableCell()()
              val typoProps = new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.subtitle1
              }
              resAcc ++= List(
                // spacer
                MuiTableRow.component.withKey("s")( MuiPropsBaseStatic.empty[MuiTableRowProps] )(
                  MuiTableCell(
                    new MuiTableCellProps {
                      val colSpan = TOTAL_COLUMNS_COUNT
                    }
                  )()
                ),
                // TOTAL-row:
                MuiTableRow.component.withKey("t")( MuiPropsBaseStatic.empty[MuiTableRowProps] )(
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
                  ),
                  // No checkbox/status here:
                  MuiTableCell()(),
                )
              )
            }
            resAcc
          }

        },

        // HTTP request in progress, render progress animation:
        props.orderContents.renderPending { _ =>
          fullRowCell(
            MuiLinearProgress()
          )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
