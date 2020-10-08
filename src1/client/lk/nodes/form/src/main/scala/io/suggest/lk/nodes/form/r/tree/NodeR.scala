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
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.nodes.{MLknOpKey, MLknOpKeys, MLknOpValue}
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
             beaconInfoR          : BeaconInfoR,
             crCtxP               : React.Context[MCommonReactCtx],
           ) {

  /** Модель props текущего компонента.
    *
    * @param node Голова этого под-дерева.
    * @param chs Нединамический указатель на список под-узлов, пробрасываемый в SubNodesR().
    * @param nodesMap Карта данных по узлам.
    */
  case class PropsVal(
                       node               : MNodeStateRender,
                       opened             : Option[NodePath_t],
                       advMode            : Boolean,
                       chs                : EphemeralStream[Tree[String]],
                       nodesMap           : Map[String, MNodeState],
                     )

  implicit val NodeRPropsValFastEq = FastEqUtil[PropsVal] { (a, b) =>
    (a.node ===* b.node) &&
    (a.opened ===* b.opened) &&
    (a.advMode ==* b.advMode) &&
    (a.chs ===* b.chs) &&
    (a.nodesMap ===* b.nodesMap)
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

    private def _onSwitchChange(optionKey: MLknOpKey)(isChecked: Boolean): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        val p = propsProxy.value
        ModifyNode(
          nodePath = Some( p.node.nodePath ),
          key = optionKey,
          value = MLknOpValue(
            bool = OptionUtil.SomeBool( isChecked ),
          ),
        )
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
      val pns = p.node.state

      val infoPot = pns.infoPot
      val infoOpt = infoPot.toOption

      // Контейнер узла узла + дочерних узлов.
      MuiTreeItem {
        // Заголовок и переключалка ля управления размещением текущей карточки в данном узле:
        val _nodeHeader = propsProxy.wrap { p =>
          nodeHeaderR.PropsVal(
            render      = p.node,
            isAdv       = p.advMode,
            chs         = p.chs,
            asList      = true,
          )
        }( nodeHeaderR.component.apply )

        val _nodeId = LknFormUtilR.nodePath2treeId( nodePath )
        new MuiTreeItemProps {
          override val nodeId = _nodeId
          override val label = _nodeHeader.rawNode
          override val onLabelClick = _onNodeLabelClickCbF
        }
      } (
        ReactCommonUtil.maybeNode(
          // Если тип n2-узла допускает рендер деталей:
          pns.role.canRenderDetails &&
          // И текущий tree-узел является открытым:
          (p.opened contains[NodePath_t] nodePath) &&
          // и есть режим формы у узла допускают рендер деталей. (СКОБКИ) ОБЯЗАТЕЛЬНЫ, иначе компилятор скомпилит феерическое нечто.
          (p.advMode match {
            // Управления узлами: детали текущего узла рендерятся всегда.
            case false =>
              true
            // Размещение карточки в узлах: Все галочки сокрыты, если главная галочка размещения выключена
            case true =>
              pns.optionBoolPot( MLknOpKeys.AdvEnabled ) contains[Boolean] true
          })
        ) {
          MuiList()(

            if (!p.advMode) {
              // ADN-режим: обычное управление ADN-узлами.
              React.Fragment(

                // Тулбар для раскрытого узла:
                infoOpt.whenDefinedNode { info =>
                  val infoProps = propsProxy.resetZoom( info )
                  nodeToolBarR.component( infoProps )
                },

                // Строка с идентификатором узла.
                infoOpt
                  .map(_.id)
                  .filter(_.nonEmpty)
                  .whenDefinedNode { infoId =>
                    MuiListItem()(
                      // Строка с идентификатором узла:
                      MuiListItemText(
                        new MuiListItemTextProps {
                          override val primary = crCtxP.message( MsgCodes.`Identifier` ).rawNode
                          override val secondary = infoId
                        }
                      )(),
                    )
                  },

                // Ряд с переключателем isEnabled узла:
                nodeEnabledR.component {
                  val enProps = nodeEnabledR.PropsVal(
                    isEnabled     = pns.optionBoolPot( MLknOpKeys.NodeEnabled ),
                    canChangeAvailability = infoPot
                      .toOption
                      .flatMap(_.isAdmin),
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
                  infoPot
                    .exists( _.ntype.exists(_.userCanCreateSubNodes) )
                ) {
                  val z2 = propsProxy.resetZoom(
                    subNodesR.PropsVal(
                      subNodes = p.chs,
                      rawNodePathRev = p.node.rawNodePathRev,
                      nodesMap = p.nodesMap,
                    )
                  )
                  subNodesR.component( z2 )
                },
              )

            } else {
              // Размещение рекламной карточки: Надо отрендерить галочки настроек размещения.
              React.Fragment(

                // Галочка "Показывать всегда раскрытой"
                nodeAdvRowR.component(
                  propsProxy.resetZoom(
                    nodeAdvRowR.PropsVal(
                      flag      = pns.optionBoolPot( MLknOpKeys.ShowOpened ),
                      msgCode   = MsgCodes.`Show.ad.opened`,
                      onChange  = _onSwitchChange( MLknOpKeys.ShowOpened ),
                    )
                  )
                ),

                // Галочка "Всегда в обводке"
                nodeAdvRowR.component(
                  propsProxy.resetZoom(
                    nodeAdvRowR.PropsVal(
                      flag      = pns.optionBoolPot( MLknOpKeys.AlwaysOutlined ),
                      msgCode   = MsgCodes.`Always.outlined`,
                      onChange  = _onSwitchChange( MLknOpKeys.AlwaysOutlined ),
                    )
                  )
                ),

              )
            },

            // Доп.инфа по BLE-маячку, если это маячок.
            {
              val bcnInfoPropsProxy = propsProxy.resetZoom {
                beaconInfoR.PropsVal(
                  nodeState = pns,
                  isAdvMode = p.advMode,
                  infoOpt   = infoOpt,
                )
              }
              beaconInfoR.component( bcnInfoPropsProxy )
            },

          )
        },

        // Под-узлы: рендерить всегда и везде - этим управляет TreeR.
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
