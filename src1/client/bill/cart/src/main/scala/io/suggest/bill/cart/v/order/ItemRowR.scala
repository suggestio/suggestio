package io.suggest.bill.cart.v.order

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
                       mitem      : MItem,
                       rowOpts    : MOrderItemRowOpts,
                       isSelected : Option[Boolean],
                       rcvrNode   : Option[MAdvGeoMapNodeProps]
                     )
  implicit object ItemRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.mitem ===* b.mitem) &&
      (a.rowOpts ===* b.rowOpts) &&
      // Инстанс Option может быть нестабильным.
      OptFastEq.Plain.eqv(a.rcvrNode, b.rcvrNode)
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

      val isSelected = props.isSelected.getOrElseFalse

      // Ключ ряда задаётся на уровне вызова компонента, не здесь.
      MuiTableRow()(

        children,

        // Определение товара/услуги:
        MuiTableCell()(
          // Если это тег, то рендерить тег:
          props.mitem.tagFaceOpt.whenDefinedNode { tagFace =>
            MuiChip {
              // Иконка тега: решётка + подсказка поверх при наведении/нажатии.
              val ava = MuiAvatar()(
                MuiToolTip(
                  new MuiToolTipProps {
                    override val title: React.Node = Messages( MsgCodes.`Tag` )
                  }
                )(
                  HtmlConstants.DIEZ
                )
              )
              new MuiChipProps {
                override val avatar = ava.rawElement
                override val label = js.defined( tagFace )
              }
            }
          },

          // Стрелочка-разделитель:
          HtmlConstants.NBSP_STR,
          Mui.SvgIcons.TrendingFlat()(),
          HtmlConstants.NBSP_STR,

          MuiChip {
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

            var avaIcon: VdomElement = avaIconComponent()()

            // Если задана подсказка, то завернуть иконку аватара в tooltip.
            for (hintMsgCode <- iconHintCodeOpt) {
              avaIcon = MuiToolTip(
                new MuiToolTipProps {
                  override val title: React.Node = hintMsgCode
                }
              )( avaIcon )
            }
            // Завернуть в avatar, как требует чип:
            avaIcon = MuiAvatar()(avaIcon)

            // Текст с описанием того, где размещение
            new MuiChipProps {
              override val avatar = avaIcon.rawElement
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
                  }
                )
                  .rawNode
              }
            }
          }
        ),

        // Колонка с ценником на изделие:
        MuiTableCell()(
          JsFormatUtil.formatPrice( props.mitem.price )
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
