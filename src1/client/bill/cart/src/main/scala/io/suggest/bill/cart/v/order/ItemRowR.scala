package io.suggest.bill.cart.v.order

import java.time.OffsetDateTime

import chandu0101.scalajs.react.components.materialui.{Mui, MuiAvatar, MuiCheckBox, MuiCheckBoxProps, MuiChip, MuiChipProps, MuiTableCell, MuiTableRow, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.bill.cart.MOrderItemRowOpts
import io.suggest.bill.price.dsl.PriceReasonI18n
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.CircleGs
import io.suggest.i18n.MsgCodes
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.bill.cart.m.CartSelectItem
import japgolly.scalajs.react.raw.React
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.dt.MYmd
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.react.r.RangeYmdR
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 13:32
  * Description: Компонент ряда одного item'а.
  */
class ItemRowR {

  /** Пропертисы для рендера одного ряда.
    *
    * @param mitem Что рендерим.
    * @param jdArgs Данные для рендера превьюшки: шаблон, эджи, параметры.
    * @param rowOpts
    * @param jdRowSpan Для колонки с превьюшкой карточки - сколько рядов можно оккупировать?
    */
  case class PropsVal(
                       mitem          : MItem,
                       rowOpts        : MOrderItemRowOpts,
                       isSelected     : Option[Boolean],
                       rcvrNode       : Option[MAdvGeoMapNodeProps],
                       isPendingReq   : Boolean,
                     )
  implicit object ItemRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.mitem ===* b.mitem) &&
      (a.rowOpts ===* b.rowOpts) &&
      (a.isSelected ==* b.isSelected) &&
      // Инстанс Option может быть нестабильным.
      OptFastEq.Plain.eqv(a.rcvrNode, b.rcvrNode) &&
      (a.isPendingReq ==* b.isPendingReq)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  private def _offsetDateTimeOpt2ymdOpt(odtOpt: Option[OffsetDateTime]): Option[MYmd] = {
    import io.suggest.dt.CommonDateTimeUtil.Implicits._
    for (odt <- odtOpt) yield
      odt.toLocalDate.toYmd
  }


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

      val isSelected = props.isSelected.getOrElseFalse

      // Ключ ряда задаётся на уровне вызова компонента, не здесь.
      MuiTableRow()(

        children,

        // Определение товара/услуги:
        MuiTableCell()(

          // Стрелочка-разделитель:
          //HtmlConstants.NBSP_STR,
          //Mui.SvgIcons.TrendingFlat()(),
          //HtmlConstants.NBSP_STR,

          {
            val (avaIconComponent, iconHintCodeOpt) = if (props.mitem.rcvrIdOpt.isDefined) {
              // Размещение в узле
              // TODO Заюзать логотип узла, если он есть + с фоном? Иконку LocationCity показывать как fallback или выкинуть вообще?
              Mui.SvgIcons.LocationCity -> Some(MsgCodes.`Node`)
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
                      .flatMap(_.hint)
                      .whenDefined,

                    // Описание гео-шейпа, если он задан:
                    props.mitem.geoShape.whenDefined {
                      // TODO + ссылка для окошка с картой и шейпом.
                      case circleGs: CircleGs =>
                        PriceReasonI18n.i18nPayloadCircle( circleGs )
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
                  rangeYmdOpt = MRangeYmdOpt(
                    dateStartOpt = _offsetDateTimeOpt2ymdOpt( props.mitem.dateStartOpt ),
                    dateEndOpt   = _offsetDateTimeOpt2ymdOpt( props.mitem.dateEndOpt )
                  )
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
        ReactCommonUtil.maybeNode( props.rowOpts.withStatus ) {
          MuiTableCell()(
            // TODO Рендерить иконку статуса
            props.mitem.status.toString
          )
        },

        // Колонка с галочкой, если требуется
        ReactCommonUtil.maybeNode( props.rowOpts.withCheckBox ) {
          MuiTableCell()(
            MuiCheckBox(
              new MuiCheckBoxProps {
                override val onChange = _itemCheckBoxChangedJsF
                override val checked = js.defined( isSelected )
                override val indeterminate = false
                override val disabled = props.isPendingReq
              }
            )
          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
