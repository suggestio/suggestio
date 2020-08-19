package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiLink, MuiLinkProps, MuiList, MuiListItem, MuiListItemText, MuiListItemTextProps, MuiToolTip, MuiToolTipProps, MuiTreeItem, MuiTreeItemProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.n2.node.MNodeTypes
import ReactCommonUtil.Implicits._
import diode.data.Pot
import io.suggest.routes.routes
import io.suggest.scalaz.NodePath_t
import ReactDiodeUtil.Implicits._
import io.suggest.lk.m.MDeleteConfirmPopupS
import io.suggest.spa.FastEqUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.17 18:28
  * Description: Рекурсивный react-компонент одной ноды дерева [[TreeR]].
  * Изначально разрастался прямо внутри [[TreeR]].
  */
class NodeR(
             nodeEnabledR         : NodeEnabledR,
             nameEditButtonR      : NameEditButtonR,
             nodeScLinkR          : NodeScLinkR,
             nodeHeaderR          : NodeHeaderR,
             deleteBtnR           : DeleteBtnR,
             tariffEditR          : TariffEditR,
             subNodesR            : SubNodesR,
             nodeAdvRowR          : NodeAdvRowR,
             crCtxP               : React.Context[MCommonReactCtx],
           ) {

  /** Модель props текущего компонента.
    *
    * @param node Голова этого под-дерева.
    */
  case class PropsVal(
                       node               : MNodeStateRender,
                       opened             : Option[NodePath_t],
                       confAdId           : Option[String],
                       deleteOpt          : Option[MDeleteConfirmPopupS],
                     )

  implicit def NodeRPropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.node ===* b.node) &&
    (a.opened ===* b.opened) &&
    (a.confAdId ===* b.confAdId) &&
    (a.deleteOpt ===* b.deleteOpt)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Клик по заголовку узла (сворачивание-разворачивание). */
    private lazy val _onNodeClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { p: Props =>
        NodeNameClick( p.value.node.nodePath )
      }
    }


    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param propsProxy Пропертисы.
      * @return React-элемент.
      */
    def render(propsProxy: Props, p: Props_t, treeChildren: PropsChildren): VdomElement = {
      val nodePath = p.node.nodePath
      val isShowProps = p.opened contains nodePath
      val isShowNodeEditProps = isShowProps && p.confAdId.isEmpty

      // Контейнер узла узла + дочерних узлов.
      MuiTreeItem {
        // Заголовок и переключалка ля управления размещением текущей карточки в данном узле:
        val _label = {
          // ADN-режим? Рендерить кнопку редактирования названия:
          val nameEditBtn = ReactCommonUtil.maybeNode( isShowNodeEditProps ) {
            propsProxy.wrap(_.node.state.editing)( nameEditButtonR.component.apply )
          }
          propsProxy.wrap( _.node )( nodeHeaderR.component(_)(nameEditBtn) )
        }
        new MuiTreeItemProps {
          override val nodeId = nodePath.mkString( HtmlConstants.`.` )
          override val label = _label.rawNode
          override val onLabelClick = _onNodeClickCbF
        }
      } (
        ReactCommonUtil.maybeEl( isShowProps ) {
          MuiList()(

            // ADN-режим: управление обычными узлами.
            if (p.confAdId.isEmpty) {
              // нет рекламной карточки - редактирование узлов:
              React.Fragment(

                MuiListItem()(
                  // Строка с идентификатором узла:
                  MuiListItemText(
                    new MuiListItemTextProps {
                      override val primary = crCtxP.message( MsgCodes.`Identifier` ).rawNode
                      override val secondary = p.node.state.info.id
                    }
                  )(),

                  // Кнопка "Перейти..." в ЛК узла:
                  ReactCommonUtil.maybeNode( p.node.state.info.ntype ==* MNodeTypes.AdnNode ) {
                    MuiToolTip(
                      new MuiToolTipProps {
                        override val title = crCtxP.message( MsgCodes.`Go.into` ).rawNode
                      }
                    )(
                      MuiLink(
                        new MuiLinkProps {
                          val href = routes.controllers.LkAds.adsPage( p.node.state.info.id ).url
                        }
                      )(
                        MuiIconButton(
                          new MuiIconButtonProps {
                            override val color = MuiColorTypes.primary
                          }
                        )(
                          Mui.SvgIcons.ArrowForward()(),
                        )
                      )
                    )
                  },

                  // Кнопка перехода в выдачу узла:
                  nodeScLinkR.component( p.node.state.info.id ),

                  // Кнопка удаления узла.
                  ReactCommonUtil.maybeEl(
                    isShowNodeEditProps &&
                    (p.node.state.info.canChangeAvailability contains[Boolean] true)
                  ) {
                    propsProxy.wrap( _.deleteOpt )( deleteBtnR.component.apply )
                  },
                ),

                // Ряд с переключателем isEnabled узла:
                propsProxy.wrap { p =>
                  val pns = p.node.state
                  nodeEnabledR.PropsVal(
                    isEnabledUpd  = pns.isEnabledUpd,
                    isEnabled     = pns.info.isEnabled,
                    request       = pns.isEnabledUpd
                      .fold[Pot[_]]( Pot.empty )(_.request),
                    canChangeAvailability = pns.info.canChangeAvailability,
                  )
                }( nodeEnabledR.component.apply ),

                // Ряд описания тарифа с кнопкой редактирования оного.
                propsProxy.wrap { p =>
                  val pns = p.node.state
                  tariffEditR.PropsVal(
                    tfDailyOpt    = pns.info.tf,
                    showExpanded  = pns.tfInfoWide,
                  )
                }( tariffEditR.component.apply ),

                // Ряд с кратким описанием подузлов и кнопкой создания оных.
                propsProxy.wrap { p =>
                  subNodesR.PropsVal(
                    chCount = treeChildren.count,
                    chCountEnabled = p.node.chCountEnabled,
                  )
                }( subNodesR.component.apply ),

              )

            } else {
              // id рекламной карточки: редактирование размещения карточки в узле. Надо отрендерить галочки настроек размещения.
              React.Fragment(

                // Галочка "Показывать всегда раскрытой"
                propsProxy.wrap { p =>
                  nodeAdvRowR.PropsVal(
                    flag      = p.node.state.advAlwaysOpenedPot,
                    msgCode   = MsgCodes.`Show.ad.opened`,
                    onChange  = AdvShowOpenedChange,
                  )
                }( nodeAdvRowR.component.apply ),

                // Галочка "Всегда в обводке"
                propsProxy.wrap { p =>
                  nodeAdvRowR.PropsVal(
                    flag      = p.node.state.advAlwaysOutlinedPot,
                    msgCode   = MsgCodes.`Always.outlined`,
                    onChange  = AlwaysOutlinedSet,
                  )
                }( nodeAdvRowR.component.apply ),

              )
            }

          )
        },

        // Под-узлы.
        treeChildren,
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackendWithChildren[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( NodeRPropsValFastEq ) )
    .build

}
