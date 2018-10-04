package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiCard, MuiCardContent, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiPaper, MuiToolTip, MuiToolTipProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.{MMdrActionInfo, MdrNextNode}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import ReactCommonUtil.Implicits._
import io.suggest.bill.price.dsl.PriceReasonI18n
import io.suggest.common.html.HtmlConstants
import io.suggest.react.r.RangeYmdR
import japgolly.scalajs.react.raw.React
import io.suggest.dt.CommonDateTimeUtil.Implicits._
import io.suggest.geo.{CircleGs, PointGs}
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.n2.edge.MPredicates

import scala.scalajs.js.UndefOr

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
                     )
  implicit object NodeMdrRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.itemsByType ===* b.itemsByType) &&
      (a.nodesMap ===* b.nodesMap) &&
      (a.directSelfNodesSorted ===* b.directSelfNodesSorted)
    }
  }


  type Props_t = Pot[Option[PropsVal]]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback клика по кнопке перезагрузки данных модерации. */
    def _refreshBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, MdrNextNode )
    lazy val _refreshBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _refreshBtnClick )


    def render(propsPotProxy: Props): VdomElement = {
      val propsPot = propsPotProxy.value

      /** Кнопка быстрой перезагрузки данных модерации. */
      def _refreshBtn = {
        MuiToolTip(
          new MuiToolTipProps {
            override val title: React.Node = Messages( MsgCodes.`Reload` )
          }
        )(
          MuiIconButton(
            new MuiIconButtonProps {
              override val onClick = _refreshBtnClickJsCbF
              override val disabled = propsPot.isPending
            }
          )(
            Mui.SvgIcons.Refresh()()
          )
        )
      }

      <.div(

        // Идёт подгрузка:
        propsPot.renderPending { _ =>
          MuiLinearProgress()
        },

        // Рендер элементов управления модерацией.
        propsPot.render { propsOpt =>

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
                  _refreshBtn
                ),
              )
            )

          } { props =>
            MuiList()(

              // Ряд полного аппрува всех item'ов вообще:
              ReactCommonUtil.maybeNode( props.itemsByType.nonEmpty || props.directSelfNodesSorted.nonEmpty ) {
                propsPotProxy.wrap { _ =>
                  mdrRowR.PropsVal(
                    actionInfo  = MMdrActionInfo(),
                    mtgVariant  = MuiTypoGraphyVariants.headline,
                    approveIcon = Mui.SvgIcons.DoneOutline,
                    dismissIcon = Mui.SvgIcons.Warning
                  )
                } { mdrRowPropsProxy =>
                  val all = MsgCodes.`All`
                  mdrRowR.component.withKey( all + HtmlConstants.UNDERSCORE )( mdrRowPropsProxy )(
                    Messages( all )
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
                    mdrRowR.PropsVal(
                      actionInfo  = MMdrActionInfo(
                        itemType = Some( itype )
                      ),
                      mtgVariant  = MuiTypoGraphyVariants.subheading,
                      approveIcon = Mui.SvgIcons.DoneAll,
                      dismissIcon = Mui.SvgIcons.Error
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
                        mdrRowR.PropsVal(
                          actionInfo = MMdrActionInfo(
                            itemId = mitem.id
                          ),
                          mtgVariant = MuiTypoGraphyVariants.body1,
                          approveIcon = Mui.SvgIcons.Done,
                          dismissIcon = Mui.SvgIcons.ErrorOutline
                        )
                      } { mdrRowPropsProxy =>
                        val itemId = mitem.id.get
                        val itemIdStr = itemId.toString

                        // Часть данных, не значимых для модерации, скрывается в tool-tip.
                        MuiToolTip(
                          new MuiToolTipProps {
                            override val title: React.Node = {
                              <.span(
                                RangeYmdR(
                                  RangeYmdR.Props(
                                    capFirst    = true,
                                    rangeYmdOpt = mitem.dtToRangeYmdOpt
                                  )
                                )
                              )
                            }
                              .rawNode
                          }
                        )(
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
                  mdrRowR.PropsVal(
                    actionInfo  = MMdrActionInfo(
                      directSelfAll = true
                    ),
                    mtgVariant  = MuiTypoGraphyVariants.headline,
                    approveIcon = Mui.SvgIcons.DoneAll,
                    dismissIcon = Mui.SvgIcons.ErrorOutline
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
                    mdrRowR.PropsVal(
                      actionInfo  = MMdrActionInfo(
                        directSelfId = Some( mnode.nodeId )
                      ),
                      mtgVariant  = MuiTypoGraphyVariants.subheading,
                      approveIcon = Mui.SvgIcons.Done,
                      dismissIcon = Mui.SvgIcons.Error
                    )
                  } { mdrRowPropsProxy =>
                    mdrRowR.component.withKey( mnode.nodeId + HtmlConstants.COMMA )( mdrRowPropsProxy )(
                      mnode.hint getOrElse[String] mnode.nodeId,
                      // TODO Ссылка на sys-узел продьюсера.
                    )
                  }
                }
                .toVdomArray

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

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsPotProxy: Props ) = component( propsPotProxy )

}
