package io.suggest.sys.mdr.v.pane

import chandu0101.scalajs.react.components.materialui.{Mui, MuiLinearProgress, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiToolTip, MuiToolTipProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.bill.price.dsl.PriceReasonI18n
import io.suggest.common.html.HtmlConstants
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.geo.{CircleGs, PointGs}
import io.suggest.i18n.MsgCodes
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodeType
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.RangeYmdR
import io.suggest.sys.mdr.MMdrActionInfo
import io.suggest.sys.mdr.v.toolbar.MdrTbStepBtnR
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 11:44
  * Description: React-компонент для кнопок управления модерацией узла (левая панель).
  */
class MdrSidePanelR(
                     mdrRowR               : MdrRowR,
                     mdrTbStepBtnR         : MdrTbStepBtnR,
                   ) {

  case class PropsVal(
                       nodeId                   : String,
                       ntypeOpt                 : Option[MNodeType],
                       itemsByType              : Map[MItemType, Seq[MItem]],
                       nodesMap                 : Map[String, MAdvGeoMapNodeProps],
                       directSelfNodesSorted    : Seq[MAdvGeoMapNodeProps],
                       mdrPots                  : Map[MMdrActionInfo, Pot[None.type]],
                       withTopOffset            : Boolean,
                     )
  implicit object NodeMdrRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.nodeId ===* b.nodeId) &&
      (a.ntypeOpt ==* b.ntypeOpt) &&
      (a.itemsByType ===* b.itemsByType) &&
      (a.nodesMap ===* b.nodesMap) &&
      (a.directSelfNodesSorted ===* b.directSelfNodesSorted) &&
      (a.mdrPots ===* b.mdrPots) &&
      (a.withTopOffset ==* b.withTopOffset)
    }
  }


  type Props_t = Pot[Option[PropsVal]]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsPotProxy: Props): VdomElement = {
      val propsPot = propsPotProxy.value

      // Кнопка быстрой перезагрузки данных модерации.
      lazy val _refreshBtn = propsPotProxy.wrap(pp => mdrTbStepBtnR.PropsVal.Refresh(pp.isPending))( mdrTbStepBtnR.apply )

      <.div(

        propsPot.renderEmpty( _refreshBtn ),

        // Рендер элементов управления модерацией.
        propsPot.render { props =>
          // Рендер, в зависимости от наличия данных в ответе. None значит нечего модерировать.
          props.whenDefinedNode { nodeProps =>
              // Краткое получение pot'а для одного элемента списка.
              def __mdrPot(ai: MMdrActionInfo) =
                nodeProps.mdrPots.getOrElse(ai, Pot.empty)

            <.div(

              // Для сдвига по вертикали в ЛК, чтобы не заезжать по топ-панель:
              ReactCommonUtil.maybe( nodeProps.withTopOffset ) {
                ^.paddingTop := 60.px
              },

              MuiList()(

                // Ряд полного аппрува всех item'ов вообще:
                ReactCommonUtil.maybeNode( nodeProps.itemsByType.nonEmpty || nodeProps.directSelfNodesSorted.nonEmpty ) {
                  val all = MsgCodes.`All`
                  propsPotProxy.wrap { _ =>
                    val ai = MMdrActionInfo()
                    mdrRowR.PropsVal(
                      actionInfo  = ai,
                      mtgVariant  = MuiTypoGraphyVariants.headline,
                      approveIcon = Mui.SvgIcons.DoneOutline,
                      dismissIcon = Mui.SvgIcons.Warning,
                      mdrPot      = __mdrPot(ai),
                    )
                  } { mdrRowPropsProxy =>
                    mdrRowR.component.withKey( all + HtmlConstants.UNDERSCORE )( mdrRowPropsProxy )(
                      Messages(all)
                    )

                  }
                },

                // Список item'ов биллинга, подлежащих модерации:
                nodeProps
                  .itemsByType
                  .iterator
                  .flatMap { case (itype, mitems) =>
                    // Ряд заголовка типа item'а.
                    val itypeCaption = propsPotProxy.wrap { _ =>
                      val ai = MMdrActionInfo(
                        itemType = Some( itype )
                      )
                      mdrRowR.PropsVal(
                        actionInfo  = ai,
                        mtgVariant  = MuiTypoGraphyVariants.subheading,
                        approveIcon = Mui.SvgIcons.DoneAll,
                        dismissIcon = Mui.SvgIcons.Error,
                        mdrPot      = __mdrPot(ai)
                      )
                    } { mdrRowPropsProxy =>
                      mdrRowR.component.withKey( itype.toString + HtmlConstants.`~` )( mdrRowPropsProxy )(
                        Messages( itype.nameI18n )
                      )
                    }

                    // Отрендерить непосредственно mitems:
                    val itemRows = mitems
                      .iterator
                      .map { mitem =>
                        propsPotProxy.wrap { _ =>
                          val ai = MMdrActionInfo(
                            itemId = mitem.id
                          )
                          mdrRowR.PropsVal(
                            actionInfo  = ai,
                            mtgVariant  = MuiTypoGraphyVariants.body1,
                            approveIcon = Mui.SvgIcons.Done,
                            dismissIcon = Mui.SvgIcons.ErrorOutline,
                            mdrPot      = __mdrPot(ai)
                          )
                        } { mdrRowPropsProxy =>
                          val itemId = mitem.id.get
                          val itemIdStr = itemId.toString

                          // Часть данных, не значимых для модерации, скрывается в tool-tip.
                          MuiToolTip.component.withKey(itemId) {
                            val _title = <.span(
                              RangeYmdR(
                                RangeYmdR.Props(
                                  capFirst    = true,
                                  rangeYmdOpt = mitem.dtToRangeYmdOpt
                                )
                              )
                            )
                            new MuiToolTipProps {
                              override val title: React.Node = _title.rawNode
                            }
                          } (
                            mdrRowR.component.withKey( itemIdStr + HtmlConstants.MINUS )(mdrRowPropsProxy)(

                              // Надо отрендерить инфу по item'у в зависимости от типа item'а.
                              HtmlConstants.DIEZ,
                              itemIdStr,
                              HtmlConstants.SPACE,

                              // Рендер названия rcvr-узла.
                              mitem.rcvrIdOpt.whenDefinedNode { rcvrId =>
                                // Рендерить название узла, присланное сервером.
                                nodeProps
                                  .nodesMap
                                  .get(rcvrId)
                                  .flatMap(_.hint)
                                  .getOrElse[String]( rcvrId )
                                  // TODO Нужна ссылка на узел-ресивер
                              },

                              // Рендерить данные по гео-размещению.
                              mitem.tagFaceOpt.whenDefinedNode { tagFace =>
                                VdomArray(
                                  HtmlConstants.SPACE,
                                  tagFace,
                                  // TODO Нужна ссылка на узел-тег.
                                )
                              },

                              // Рендерить данные гео-шейпа.
                              mitem.geoShape.whenDefinedNode {
                                // TODO Сделать ссылкой, открывающий диалог с картой и шейпом.
                                case circleGs: CircleGs =>
                                  PriceReasonI18n.i18nPayloadCircle( circleGs )
                                // PointGs здесь - Это скорее запасной костыль и для совместимости, нежели какое-то нужное обоснованное логичное решение.
                                case point: PointGs =>
                                  point.coord.toHumanFriendlyString
                                case other =>
                                  VdomArray(
                                    other.getClass.getSimpleName,
                                    HtmlConstants.DIEZ,
                                    other.hashCode()
                                  )
                              }

                            )
                          )

                        }
                      }
                      .toStream

                    itypeCaption #:: itemRows
                  }
                  .toVdomArray,

                // Рендер своих узлов-ресиверов бесплатного размещения.
                // Сначала заголовок для бесплатных размещений:
                ReactCommonUtil.maybeNode( nodeProps.directSelfNodesSorted.nonEmpty ) {
                  propsPotProxy.wrap { _ =>
                    val ai = MMdrActionInfo(
                      directSelfAll = true
                    )
                    mdrRowR.PropsVal(
                      actionInfo  = ai,
                      mtgVariant  = MuiTypoGraphyVariants.headline,
                      approveIcon = Mui.SvgIcons.DoneAll,
                      dismissIcon = Mui.SvgIcons.ErrorOutline,
                      mdrPot      = __mdrPot(ai),
                    )
                  } { mdrRowPropsProxy =>
                    mdrRowR.component( mdrRowPropsProxy )(
                      // TODO Несовсем точно, т.к. размещение на продьюсере не является тем, что необходимо.
                      Messages( MPredicates.Receiver.Self.singular )
                    )
                  }
                },

                // Список размещений
                nodeProps
                  .directSelfNodesSorted
                  .map { mnode =>
                    propsPotProxy.wrap { _ =>
                      val ai = MMdrActionInfo(
                        directSelfId = Some( mnode.nodeId )
                      )
                      mdrRowR.PropsVal(
                        actionInfo  = ai,
                        mtgVariant  = MuiTypoGraphyVariants.subheading,
                        approveIcon = Mui.SvgIcons.Done,
                        dismissIcon = Mui.SvgIcons.Error,
                        mdrPot      = __mdrPot(ai)
                      )
                    } { mdrRowPropsProxy =>
                      mdrRowR.component.withKey( mnode.nodeId + HtmlConstants.COMMA )( mdrRowPropsProxy )(
                        mnode.hintOrId,
                        // TODO Ссылка на sys-узел продьюсера.
                      )
                    }
                  }
                  .toVdomArray

              )
            )
          }
        },

        // Рендер ошибок:
        propsPot.renderFailed { ex =>
          MuiToolTip(
            new MuiToolTipProps {
              override val title: React.Node = ex.toString
            }
          )(
            MuiListItem()(
              MuiListItemIcon()(
                Mui.SvgIcons.Error()()
              ),

              MuiListItemText()(
                Messages( MsgCodes.`Something.gone.wrong` )
              ),
              // TODO Кнопка релоада: сделать onClick:
              _refreshBtn
            )
          )
        },

        // Идёт подгрузка:
        propsPot.renderPending { _ =>
          <.div(
            <.br,
            MuiLinearProgress()
          )
        },

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsPotProxy: Props ) = component( propsPotProxy )

}
