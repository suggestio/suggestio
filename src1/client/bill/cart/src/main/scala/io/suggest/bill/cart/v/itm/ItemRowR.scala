package io.suggest.bill.cart.v.itm

import com.materialui.{Mui, MuiAvatar, MuiCheckBox, MuiCheckBoxProps, MuiChip, MuiChipProps, MuiColorTypes, MuiTableCell, MuiTableCellClasses, MuiTableCellProps, MuiTableRow, MuiToolTip, MuiToolTipProps}
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
  * Description: Single item row react-component.
  */
class ItemRowR(
                orderCss: OrderCss
              ) {

  /** Single row properties.
    *
    * @param mitem Current item.
    * @param isItemEditable Is item editable?
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

      MuiTableRow()(

        // Ad preview miniature table-cell, if any.
        props.previewRowSpan.whenDefinedNode { previewRowSpan =>
          MuiTableCell {
            val previewCssClasses = new MuiTableCellClasses {
              override val root = orderCss.ItemsTable.NodePreviewColumn.body.htmlClass
            }
            new MuiTableCellProps {
              // td.rowspan directly passed into HTML tag attrs.
              val rowSpan = previewRowSpan.toString
              override val classes = previewCssClasses
            }
          }(
            children
          )
        },

        // Cell with order item definition:
        MuiTableCell()(

          {
            val (avaIconComponent, iconHintCodeOpt) = if (props.mitem.rcvrIdOpt.isDefined) {
              // Advertising inside node.
              val ntypeOpt = props.rcvrNode
                .flatMap(_.ntype)

              val nodeTypeHint = (for {
                ntype <- ntypeOpt
                if !(ntype eqOrHasParent MNodeTypes.AdnNode)
              } yield {
                ntype.singular
              })
                .getOrElse( MsgCodes.`Node` )

              // Separate icon for BLE-beacon.
              val nodeIcon = ntypeOpt
                .flatMap { ntype =>
                  Option {
                    if (ntype eqOrHasParent MNodeTypes.BleBeacon)
                      Mui.SvgIcons.BluetoothSearching
                    else if (ntype eqOrHasParent MNodeTypes.WifiAP)
                      Mui.SvgIcons.Wifi
                    else
                      null
                  }
                }
                .getOrElse( Mui.SvgIcons.LocationCity )

              nodeIcon -> Some(nodeTypeHint)
            } else if (props.mitem.geoShape.isDefined) {
              // On the map
              Mui.SvgIcons.MyLocation -> Some(MsgCodes.`Adv.on.map`)
            } else {
              // Unknown location type. Should not happen.
              Mui.SvgIcons.NotListedLocation -> None
            }

            var chip: VdomElement = MuiChip {
              // Description about where is advertise placement occurs.
              new MuiChipProps {
                // Wrap into avatar, as needed by MuiChip:
                override val avatar = {
                  MuiAvatar()(
                    avaIconComponent()()
                  ).rawElement
                }
                override val label: js.UndefOr[React.Node] = {
                  <.span(
                    // Name of receiver (target) node, if any.
                    props.rcvrNode
                      .flatMap(_.name)
                      .whenDefined,

                    // Geo-shape description, if defined.
                    props.mitem.geoShape.whenDefined {
                      // TODO + link to window with geo-map and shape.
                      case circleGs: CircleGs =>
                        PriceReasonI18n.i18nPayloadCircle( circleGs )
                      // PointGs here. Possibly, for compatibility reasons or something like.
                      case point: PointGs =>
                        point.coord.toHumanFriendlyString
                      case other =>
                        // TODO Provide any normal render here for other geo-shapes.
                        other.toString()
                    },

                    // If #tag is here...
                    props.mitem.tagFaceOpt.whenDefinedNode { tagFace =>
                      VdomArray(
                        HtmlConstants.NBSP_STR,
                        MuiToolTip.component.withKey("t")(
                          new MuiToolTipProps {
                            override val title: React.Node = Messages( MsgCodes.`Tag` )
                          }
                        )(
                          MuiChip {
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

            // If some icon hint defined, make a tooltip:
            for (hintMsgCode <- iconHintCodeOpt) {
              chip = MuiToolTip(
                new MuiToolTipProps {
                  override val title: React.Node = Messages( hintMsgCode )
                }
              )( chip )
            }

            chip
          },

          // Render start/end dates as secondary string, if any.
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

        // Column with item price:
        MuiTableCell()(
          JsFormatUtil
            .formatPrice( props.mitem.price )
            .spacingToVdomNbspStrings
        ),

        if (props.isItemEditable) {
          // Selection checkbox, if item editing allowed:
          MuiTableCell()(
            MuiCheckBox(
              new MuiCheckBoxProps {
                override val onChange = _itemCheckBoxChangedJsF
                override val checked = js.defined( props.isSelected.getOrElseFalse )
                override val indeterminate = false
                override val disabled = props.isPendingReq
                override val color = MuiColorTypes.secondary
              }
            )
          )
        } else {
          // Column with current item status (see MItemStatuses):
          MuiTableCell()(
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
