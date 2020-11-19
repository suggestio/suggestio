package io.suggest.bill.cart.v.itm

import com.materialui.{Mui, MuiAvatar, MuiCheckBox, MuiCheckBoxProps, MuiChip, MuiChipProps, MuiTableCell, MuiTableCellClasses, MuiTableCellProps, MuiTableRow, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.m.CartSelectItem
import io.suggest.bill.cart.v.order.OrderCss
import io.suggest.bill.price.dsl.PriceReasonI18n
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.{CircleGs, PointGs}
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.n2.node.MNodeTypes
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.RangeYmdR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.sc.index.MSc3IndexResp

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 13:32
  * Description: Компонент ряда одного item'а.
  */
class ItemRowR(
                orderCss: OrderCss
              ) {

  /** Пропертисы для рендера одного ряда.
    *
    * @param mitem Что рендерим.
    * @param isItemEditable Можно ли управлять item'ом?
    */
  case class PropsVal(
                       mitem          : MItem,
                       isItemEditable : Boolean,
                       isSelected     : Option[Boolean],
                       rcvrNode       : Option[MSc3IndexResp],
                       isPendingReq   : Boolean,
                       previewRowSpan : Option[Int]
                     )
  implicit object ItemRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.mitem ===* b.mitem) &&
      (a.isItemEditable ==* b.isItemEditable) &&
      (a.isSelected ==* b.isSelected) &&
      // Инстанс Option может быть нестабильным.
      OptFastEq.Plain.eqv(a.rcvrNode, b.rcvrNode) &&
      (a.isPendingReq ==* b.isPendingReq) &&
      OptFastEq.OptValueEq.eqv(a.previewRowSpan, b.previewRowSpan)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    private def _itemCheckBoxChanged(e: ReactEventFromInput): Callback = {
      val isSelected = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf( $ ) { propsProxy: Props =>
        CartSelectItem(propsProxy.value.mitem.id, isSelected)
      }
    }
    private lazy val _itemCheckBoxChangedJsF = ReactCommonUtil.cbFun1ToJsCb( _itemCheckBoxChanged )


    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      val props = propsProxy.value

      // Ключ ряда задаётся на уровне вызова компонента, не здесь.
      MuiTableRow()(

        // Рендер ячейки для миниатюры карточки, если задана.
        props.previewRowSpan.whenDefinedNode { previewRowSpan =>
          MuiTableCell {
            val previewCssClasses = new MuiTableCellClasses {
              override val root = orderCss.ItemsTable.NodePreviewColumn.body.htmlClass
            }
            new MuiTableCellProps {
              // Передаём rowspan напрямую в атрибуты td:
              val rowSpan = previewRowSpan.toString
              override val classes = previewCssClasses
            }
          }(
            children
          )
        },

        // Определение товара/услуги:
        MuiTableCell()(

          {
            val (avaIconComponent, iconHintCodeOpt) = if (props.mitem.rcvrIdOpt.isDefined) {
              // Размещение в узле. Отдельная иконка для ble-маячка
              val ntypeOpt = props.rcvrNode
                .flatMap(_.ntype)

              val nodeTypeHint = (for {
                ntype <- ntypeOpt
                if !(ntype eqOrHasParent MNodeTypes.AdnNode)
              } yield {
                ntype.singular
              })
                .getOrElse( MsgCodes.`Node` )

              val nodeIcon = (for {
                ntype <- ntypeOpt
                if ntype eqOrHasParent MNodeTypes.BleBeacon
              } yield {
                Mui.SvgIcons.BluetoothSearching
              })
                .getOrElse( Mui.SvgIcons.LocationCity )

              nodeIcon -> Some(nodeTypeHint)
            } else if (props.mitem.geoShape.isDefined) {
              // На карте
              Mui.SvgIcons.MyLocation -> Some(MsgCodes.`Adv.on.map`)
            } else {
              // Непонятный тип локации. Не должно такого происходить.
              Mui.SvgIcons.NotListedLocation -> None
            }

            var chip: VdomElement = MuiChip {
              // Текст с описанием того, где размещение
              new MuiChipProps {
                // Завернуть в avatar, как требует чип:
                override val avatar = {
                  MuiAvatar()(
                    avaIconComponent()()
                  ).rawElement
                }
                override val label: js.UndefOr[React.Node] = {
                  <.span(
                    // Название узла-ресивера, если задан
                    props.rcvrNode
                      .flatMap(_.name)
                      .whenDefined,

                    // Описание гео-шейпа, если он задан:
                    props.mitem.geoShape.whenDefined {
                      // TODO + ссылка для окошка с картой и шейпом.
                      case circleGs: CircleGs =>
                        PriceReasonI18n.i18nPayloadCircle( circleGs )
                      // PointGs здесь - Это скорее запасной костыль и для совместимости, нежели какое-то нужное обоснованное логичное решение.
                      case point: PointGs =>
                        point.coord.toHumanFriendlyString
                      case other =>
                        // TODO Нужен какой-то нормальный рендер, не?
                        other.toString()
                    },

                    // Если это тег, то рендерить тег:
                    props.mitem.tagFaceOpt.whenDefinedNode { tagFace =>
                      VdomArray(
                        HtmlConstants.NBSP_STR,
                        MuiToolTip.component.withKey("t")(
                          new MuiToolTipProps {
                            override val title: React.Node = Messages( MsgCodes.`Tag` )
                          }
                        )(
                          MuiChip {
                            // Иконка тега: решётка + подсказка поверх при наведении/нажатии.
                            val ava = MuiAvatar()(
                              HtmlConstants.DIEZ
                            )
                            new MuiChipProps {
                              override val avatar = ava.rawElement
                              override val label = js.defined( tagFace )
                            }
                          }
                        )
                      )
                    }
                  )
                    .rawNode
                }
              }
            }

            // Если задана подсказка, то завернуть иконку аватара в tooltip.
            for (hintMsgCode <- iconHintCodeOpt) {
              chip = MuiToolTip(
                new MuiToolTipProps {
                  override val title: React.Node = Messages( hintMsgCode )
                }
              )( chip )
            }

            chip
          },

          // Если есть даты end/start, то вторая строка:
          ReactCommonUtil.maybeNode( props.mitem.dateStartOpt.isDefined || props.mitem.dateEndOpt.isDefined )(
            VdomArray(
              <.br(
                ^.key := "b"
              ),
              RangeYmdR.component.withKey("r")(
                RangeYmdR.Props(
                  capFirst = false,
                  rangeYmdOpt = props.mitem.dtToRangeYmdOpt
                )
              )
            )
          )

        ),

        // Колонка с ценником на изделие:
        MuiTableCell()(
          JsFormatUtil
            .formatPrice( props.mitem.price )
            .spacingToVdomNbspStrings
        ),

        // Колонка со статусом обработки item'а:
        if (props.isItemEditable) {
          // Колонка с галочкой, если требуется
          MuiTableCell()(
            MuiCheckBox(
              new MuiCheckBoxProps {
                override val onChange = _itemCheckBoxChangedJsF
                override val checked = js.defined( props.isSelected.getOrElseFalse )
                override val indeterminate = false
                override val disabled = props.isPendingReq
              }
            )
          )
        } else {
          // Колонка со статусом:
          MuiTableCell()(
            // TODO Рендерить иконку статуса
            MuiToolTip(
              new MuiToolTipProps {
                override val title: React.Node = {
                  <.span(
                    Messages( props.mitem.status.nameI18n ),
                    props.mitem.reasonOpt.whenDefinedNode { reason =>
                      <.span(
                        <.br,
                        reason
                      )
                    }
                  )
                }
                  .rawNode
              }
            )(
              props.mitem.status match {
                case MItemStatuses.Finished       => Mui.SvgIcons.Done()()
                case MItemStatuses.Online         => Mui.SvgIcons.PlayArrow()()
                case MItemStatuses.Offline        => Mui.SvgIcons.AccessAlarm()()
                case MItemStatuses.AwaitingMdr    => Mui.SvgIcons.AccessTime()()
                case MItemStatuses.Refused        => Mui.SvgIcons.Report()()
                case MItemStatuses.Draft          => Mui.SvgIcons.Edit()()
              }
            )
          )
        },

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
