package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiList, MuiListItem, MuiListItemText, MuiListItemTextProps, MuiTreeItem, MuiTreeItemProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import ReactDiodeUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import diode.data.Pot
import io.suggest.scalaz.NodePath_t
import io.suggest.lk.nodes.form.u.LknFormUtilR
import io.suggest.spa.FastEqUtil
import scalaz.{EphemeralStream, Tree}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.17 18:28
  * Description: Рекурсивный react-компонент одной ноды дерева [[TreeR]].
  * Изначально разрастался прямо внутри [[TreeR]].
  */
class NodeR(
             nodeEnabledR         : NodeEnabledR,
             nodeHeaderR          : NodeHeaderR,
             nodeToolBarR         : NodeToolBarR,
             tariffEditR          : TariffEditR,
             subNodesR            : SubNodesR,
             nodeAdvRowR          : NodeAdvRowR,
             crCtxP               : React.Context[MCommonReactCtx],
           ) {

  /** Модель props текущего компонента.
    *
    * @param node Голова этого под-дерева.
    * @param chs Нединамический указатель на список под-узлов, пробрасываемый в SubNodesR().
    */
  case class PropsVal(
                       node               : MNodeStateRender,
                       opened             : Option[NodePath_t],
                       advMode            : Boolean,
                       chs                : EphemeralStream[Tree[MNodeState]],
                     )

  implicit val NodeRPropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.node ===* b.node) &&
    (a.opened ===* b.opened) &&
    (a.advMode ==* b.advMode) &&
    (a.chs ===* b.chs)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Клик по заголовку узла (сворачивание-разворачивание). */
    private lazy val _onNodeLabelClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { p: Props =>
        NodeClick( p.value.node.nodePath )
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

      // Контейнер узла узла + дочерних узлов.
      MuiTreeItem {
        // Заголовок и переключалка ля управления размещением текущей карточки в данном узле:
        val _label = propsProxy.wrap { p =>
          nodeHeaderR.PropsVal(
            render      = p.node,
            isAdv       = p.advMode,
          )
        }( nodeHeaderR.component.apply )

        val _nodeId = LknFormUtilR.nodePath2treeId( nodePath )
        new MuiTreeItemProps {
          override val nodeId = _nodeId
          override val label = _label.rawNode
          override val onLabelClick = _onNodeLabelClickCbF
        }
      } (
        ReactCommonUtil.maybeEl(
          p.node.state.role.canRenderDetails &&
          (p.opened contains[NodePath_t] nodePath)
        ) {

          if (!p.advMode) {
            // ADN-режим: обычное управление ADN-узлами.
            val pns = p.node.state
            val infoOpt = pns.infoPot.toOption

            MuiList()(

              // Тулбар для раскрытого узла:
              infoOpt.whenDefinedNode { info =>
                nodeToolBarR.component(
                  propsProxy.resetZoom( info )
                )
              },

              // Строка с идентификатором узла.
              infoOpt
                .filter(_.id.nonEmpty)
                .whenDefinedNode { info =>
                  MuiListItem()(
                    // Строка с идентификатором узла:
                    MuiListItemText(
                      new MuiListItemTextProps {
                        override val primary = crCtxP.message( MsgCodes.`Identifier` ).rawNode
                        override val secondary = info.id
                      }
                    )(),
                  )
                },

              // Ряд с переключателем isEnabled узла:
              nodeEnabledR.component {
                val enProps = nodeEnabledR.PropsVal(
                  isEnabledUpd  = pns.isEnabledUpd,
                  isEnabled     = pns.infoPot
                    .exists(_.isEnabled),
                  request       = pns.isEnabledUpd
                    .fold[Pot[_]]( Pot.empty )(_.request),
                  canChangeAvailability = pns.infoPot
                    .toOption
                    .flatMap(_.canChangeAvailability),
                )
                propsProxy.resetZoom( enProps )
              },

              // Ряд описания тарифа с кнопкой редактирования оного.
              tariffEditR.component {
                val tfProps = tariffEditR.PropsVal(
                  tfDailyOpt    = infoOpt.flatMap(_.tf),
                  showExpanded  = pns.tfInfoWide,
                )
                propsProxy.resetZoom( tfProps )
              },

              // Ряд с кратким описанием под-узлов и кнопкой создания оных:
              ReactCommonUtil.maybeNode(
                p.node.state.infoPot
                  .exists( _.ntype.userCanCreateSubNodes )
              ) {
                val z2 = propsProxy.resetZoom( p.chs )
                subNodesR.component( z2 )
              },

            )

          } else {
            // id рекламной карточки: редактирование размещения карточки в узле. Надо отрендерить галочки настроек размещения.
            ReactCommonUtil.maybeEl( p.node.state.advHasAdvPot contains[Boolean] true ) {
              MuiList()(

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
          }

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
