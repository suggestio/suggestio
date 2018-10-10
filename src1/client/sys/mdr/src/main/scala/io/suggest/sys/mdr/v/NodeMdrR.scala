package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiCard, MuiCardContent, MuiDivider, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiPaper, MuiSvgIcon, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.MdrNextNode
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import ReactCommonUtil.Implicits._
import io.suggest.bill.price.dsl.PriceReasonI18n
import io.suggest.common.html.HtmlConstants
import io.suggest.react.r.RangeYmdR
import japgolly.scalajs.react.raw.React
import japgolly.univeq._
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.geo.{CircleGs, PointGs}
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.n2.edge.MPredicates
import io.suggest.sys.mdr.MMdrActionInfo

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 11:44
  * Description: React-компонент для кнопок управления модерацией узла (левая панель).
  */
class NodeMdrR(
                mdrRowR: MdrRowR
              ) {

  case class PropsVal(
                       itemsByType              : Map[MItemType, Seq[MItem]],
                       nodesMap                 : Map[String, MAdvGeoMapNodeProps],
                       directSelfNodesSorted    : Seq[MAdvGeoMapNodeProps],
                       mdrPots                  : Map[MMdrActionInfo, Pot[None.type]],
                       nodeOffset               : Int,
                     )
  implicit object NodeMdrRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.itemsByType ===* b.itemsByType) &&
      (a.nodesMap ===* b.nodesMap) &&
      (a.directSelfNodesSorted ===* b.directSelfNodesSorted) &&
      (a.mdrPots ===* b.mdrPots) &&
      (a.nodeOffset ==* b.nodeOffset)
    }
  }


  type Props_t = Pot[Option[PropsVal]]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по кнопке перезагрузки данных модерации. */
    private def _refreshBtnClick(offsetDelta: Int)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, MdrNextNode(offsetDelta = offsetDelta) )
    private lazy val _previousNodeBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _refreshBtnClick(-1) )
    private lazy val _refreshBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _refreshBtnClick(0) )
    private lazy val _nextNodeBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _refreshBtnClick(+1) )


    def render(propsPotProxy: Props): VdomElement = {
      val propsPot = propsPotProxy.value

      // Сборка icon-кнопки с подсказкой.
      def _btn(titleMsgCode: String, icon: MuiSvgIcon, isDisabled: Boolean = false)(cb: js.Function1[ReactEvent, Unit]): VdomElement = {
        MuiToolTip {
          val _title = Messages( titleMsgCode ): React.Node
          new MuiToolTipProps {
            override val title: React.Node = _title
          }
        } (
          MuiIconButton(
            new MuiIconButtonProps {
              override val onClick = cb
              override val disabled = isDisabled || propsPot.isPending
            }
          )(
            icon()()
          )
        )
      }

      // Кнопка быстрой перезагрузки данных модерации.
      val _refreshBtn = _btn( MsgCodes.`Reload`, Mui.SvgIcons.Refresh )( _refreshBtnClickJsCbF )

      <.div(

        propsPot.renderEmpty( _refreshBtn ),

        // Идёт подгрузка:
        propsPot.renderPending { _ =>
          MuiLinearProgress()
        },

        // Рендер элементов управления модерацией.
        propsPot.render { propsOpt =>

          <.div(

            <.span(
              _btn( MsgCodes.`Previous.node`, Mui.SvgIcons.SkipPrevious, propsOpt.fold(false)(_.nodeOffset <= 0) )( _previousNodeBtnClickJsCbF ),
              _refreshBtn,
              _btn( MsgCodes.`Next.node`, Mui.SvgIcons.SkipNext, propsOpt.isEmpty )( _nextNodeBtnClickJsCbF ),
            ),

            MuiDivider(),

            // Рендер, в зависимости от наличия данных в ответе. None значит нечего модерировать.
            propsOpt.fold[VdomNode] {
              // TODO Подверстать, чтобы по центру всё было (и контент, и контейнер).
              MuiPaper()(
                MuiCard()(
                  MuiCardContent()(
                    Mui.SvgIcons.WbSunny()(),
                    MuiTypoGraphy(
                      new MuiTypoGraphyProps {
                        override val variant = MuiTypoGraphyVariants.headline
                      }
                    )(
                      Messages( MsgCodes.`Nothing.found` ),
                    ),
                    <.br,
                    _refreshBtn,
                  ),
                )
              )

            } { props =>
              // Краткое получение pot'а для одного элемента списка.
              def __mdrPot(ai: MMdrActionInfo) =
                props.mdrPots.getOrElse(ai, Pot.empty)

              MuiList()(

                // Ряд полного аппрува всех item'ов вообще:
                ReactCommonUtil.maybeNode( props.itemsByType.nonEmpty || props.directSelfNodesSorted.nonEmpty ) {
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
                props
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
                                props
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
                ReactCommonUtil.maybeNode( props.directSelfNodesSorted.nonEmpty ) {
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
                props
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
            }
          )
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

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsPotProxy: Props ) = component( propsPotProxy )

}
