package io.suggest.bill.cart.v.itm

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiTableBody, MuiTableCell, MuiTableCellClasses, MuiTableCellProps, MuiTableRow, MuiTableRowProps, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.bill.cart.m.LoadCurrentOrder
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.bill.cart.MOrderContent
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
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
  * Description: Компонент наполнения списка-таблицы заказа.
  */
class ItemsTableBodyR(
                       orderCss               : OrderCss,
                       jdR                    : JdR,
                       val itemRowR           : ItemRowR,
                     ) {

  /** Модель пропертисов компонента.
    *
    * @param items item'ы в ордере.
    * @param rcvrs Карта узлов для рендера названий ресиверов и остального.
    */
  case class PropsVal(
                       orderContents  : Pot[MOrderContent],
                       selectedIds    : Set[Gid_t],
                       jdCss          : JdCss,
                     )
  implicit object ItemsTableBodyRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.orderContents      ===* b.orderContents) &&
        (a.selectedIds      ===* b.selectedIds) &&
        (a.jdCss            ===* b.jdCss)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  /** Тело компонента, занимающегося рендером тела списка item'ов. */
  class Backend($: BackendScope[Props, Unit]) {

    private def _onRefreshBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, LoadCurrentOrder)
    lazy val _onRefreshBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onRefreshBtnClick )


    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      // Сколько колонок всего? Не всегда это важно.
      val columnsCount = 4

      // Пропертисы для fullRowCell:
      lazy val fullRowCellProps = {
        val css = new MuiTableCellClasses {
          override val root = orderCss.ItemsTable.TBody.fullRowCell.htmlClass
        }
        new MuiTableCellProps {
          override val classes = css
          val colSpan = columnsCount
        }
      }

      def fullRowCell(cells: VdomNode*) =
        MuiTableRow()(
          MuiTableCell(
            fullRowCellProps
          )(cells: _*)
        )

      MuiTableBody()(

        // TODO renderEmpty - что рендерить? По идее, это не нужно, т.к. никогда не должно быть видимо Pot.empty.

        // Какие-то проблемы со список item'ов - ошибка и предложение перезагрузки.
        props.orderContents.renderFailed { ex =>
          fullRowCell(
            // Уведомление о проблеме.
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

            // Кнопка перезагрузки...
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
              previewOpt = {
                // Пытаемся отрендерить как jd-карточку:
                orderContent
                  .adId2jdDataMap
                  .get( nodeId )
                  .map[VdomElement] { jdAdData =>
                    propsProxy.wrap { _ =>
                      MJdArgs(
                        template = jdAdData.template,
                        edges = jdAdData.edgesMap
                          // TODO Opt Инстанс карты с js-эджами следует собирать не тут, а в контроллере и хранить в состоянии.
                          .mapValues(MEdgeDataJs(_)),
                        jdCss = props.jdCss,
                        conf  = props.jdCss.jdCssArgs.conf
                      )
                    }( jdR.apply )
                  }
                  .orElse {
                    // Поискать в adn node logos
                    orderContent
                      .adnNodesMap
                      .get( nodeId )
                      .map[VdomElement] { nodeProps =>
                        // Рендерим видимую часть: иконка или что-то ещё.
                        val nodeName = nodeProps.hintOrId
                        MuiToolTip {
                          new MuiToolTipProps {
                            override val title: React.Node = nodeName
                          }
                        } {
                          nodeProps.icon
                            .map[VdomElement] { icon =>
                              <.img(
                                orderCss.ItemsTable.NodePreviewColumn.adnLogo,
                                ^.src := icon.url,
                                // Это нужно для logo, и вообще не нужно для wcAsLogo, TODO отработать как-то эту ситуацию?
                                //nodeProps.colors.bg.whenDefined { mcd =>
                                //  ^.backgroundColor := mcd.hexCode
                                //}
                              )
                            }
                            .getOrElse[VdomElement] {
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

              // Пройти item'ы, наконец:
              (mitem, i) <- nodeItems.iterator.zipWithIndex

            } yield {
              val isWithPreview = (i ==* 0)
              propsProxy.wrap { _ =>
                itemRowR.PropsVal(
                  mitem           = mitem,
                  isItemEditable  = orderContent.isItemsEditable,
                  isSelected      = mitem.id.map(props.selectedIds.contains),
                  rcvrNode        = mitem.rcvrIdOpt.flatMap( orderContent.adnNodesMap.get ),
                  isPendingReq    = props.orderContents.isPending,
                  previewRowSpan  = OptionUtil.maybe(isWithPreview)(nodeItemsCount)
                )
              } { itemPropsValProxy =>
                itemRowR.component
                  .withKey(nodeId + HtmlConstants.UNDERSCORE + mitem.id.get)( itemPropsValProxy )(
                    // Рендерить первую ячейку с превьюшкой надо только в первом ряду, остальные ряды - rowspan'ятся
                    ReactCommonUtil.maybeNode( isWithPreview )( previewOpt )
                  )
              }
            }
            val resAcc = iter.toVdomArray

            // Если заданы order-prices, то надо ещё отрендерить ряд ИТОГО
            if (orderContent.orderPrices.nonEmpty) {
              val emptyCell = MuiTableCell()()
              val typoProps = new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.subtitle1
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
                  ),
                  // В ячейке чек-бокса или статуса - пока ничего.
                  MuiTableCell()()
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