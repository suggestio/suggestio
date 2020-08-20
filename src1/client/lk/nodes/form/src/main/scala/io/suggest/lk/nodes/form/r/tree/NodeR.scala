package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiList, MuiListItem, MuiListItemText, MuiListItemTextProps, MuiTreeItem, MuiTreeItemProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import ReactCommonUtil.Implicits._
import diode.data.Pot
import io.suggest.scalaz.NodePath_t
import ReactDiodeUtil.Implicits._
import io.suggest.lk.nodes.form.u.LknFormUtilR
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
             nodeHeaderR          : NodeHeaderR,
             tariffEditR          : TariffEditR,
             subNodesR            : SubNodesR,
             nodeAdvRowR          : NodeAdvRowR,
             crCtxP               : React.Context[MCommonReactCtx],
           ) {

  /** Модель props текущего компонента.
    *
    * @param locked Блокировать все лишние кнопки? Например, какой-то диалог открыт.
    * @param node Голова этого под-дерева.
    */
  case class PropsVal(
                       node               : MNodeStateRender,
                       opened             : Option[NodePath_t],
                       confAdId           : Option[String],
                       locked             : Boolean,
                     )

  implicit def NodeRPropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.node ===* b.node) &&
    (a.opened ===* b.opened) &&
    (a.confAdId ===* b.confAdId) &&
    (a.locked ==* b.locked)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Клик по заголовку узла (сворачивание-разворачивание). */
    private lazy val _onNodeLabelClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
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
      val isShowProps = p.opened contains[NodePath_t] nodePath
      val isShowNodeEditProps = isShowProps && p.confAdId.isEmpty

      // Контейнер узла узла + дочерних узлов.
      MuiTreeItem {
        // Заголовок и переключалка ля управления размещением текущей карточки в данном узле:
        val _label = propsProxy.wrap { p =>
          nodeHeaderR.PropsVal(
            render      = p.node,
            isShowProps = isShowNodeEditProps,
            locked      = p.locked,
          )
        }( nodeHeaderR.component.apply )

        val _nodeId = LknFormUtilR.nodePath2treeId( nodePath )
        new MuiTreeItemProps {
          override val nodeId = _nodeId
          override val label = _label.rawNode
          override val onLabelClick = _onNodeLabelClickCbF
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
            },

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
