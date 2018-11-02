package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{MuiDrawer, MuiDrawerAnchors, MuiDrawerProps, MuiDrawerVariants, MuiToolBar}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.jd.render.v.{JdCss, JdCssR}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.spa.{DiodeUtil, OptFastEq}
import io.suggest.sys.mdr.m.MSysMdrRootS
import io.suggest.sys.mdr.v.dia.MdrDiaRefuseR
import io.suggest.sys.mdr.v.main.{MdrErrorsR, NodeRenderR}
import io.suggest.sys.mdr.v.pane.MdrSidePanelR
import io.suggest.sys.mdr.v.toolbar.{MdrForceAllNodesR, MdrToolBarR}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 11:00
  * Description: react-компонент формы модерации одного узла.
  * Состоит из двух панелей, кнопок аппрува/отказа + отрендеренными данными по узлу.
  */
class SysMdrFormR(
                   val mdrSidePanelR      : MdrSidePanelR,
                   val nodeRenderR        : NodeRenderR,
                   val mdrErrorsR         : MdrErrorsR,
                   val mdrDiaRefuseR      : MdrDiaRefuseR,
                   val mdrToolBarR        : MdrToolBarR,
                   val mdrForceAllNodesR  : MdrForceAllNodesR,
                   jdCssR                 : JdCssR,
                 ) {

  type Props_t = MSysMdrRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    jdCssC              : ReactConnectProxy[JdCss],
                    nodeInfoC           : ReactConnectProxy[mdrSidePanelR.Props_t],
                    nodeRenderC         : ReactConnectProxy[nodeRenderR.Props_t],
                    mdrErrorsC          : ReactConnectProxy[mdrErrorsR.Props_t],
                    diaRefuseC          : ReactConnectProxy[mdrDiaRefuseR.Props_t],
                    mdrToolBarC         : ReactConnectProxy[mdrToolBarR.Props_t],
                    mdrForceAllNodesC   : ReactConnectProxy[mdrForceAllNodesR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      // Панель управления модерации.
      <.div(

        // Рендер css карточки:
        s.jdCssC { jdCssR.apply },

        // Тулбар, без AppBar, т.к. он неуместен и делает неконтрастный фон.
        MuiToolBar()(
          // Основной тулбар
          s.mdrToolBarC { mdrToolBarR.apply },
          // Галочка форсирования выхода за пределы узла.
          s.mdrForceAllNodesC { mdrForceAllNodesR.apply },
        ),

        <.br,

        // Ошибки - здесь:
        s.mdrErrorsC { mdrErrorsR.apply },
        <.br,

        // Левая панель:
        MuiDrawer(
          new MuiDrawerProps {
            override val variant = MuiDrawerVariants.permanent
            override val anchor  = MuiDrawerAnchors.right
          }
        )(
          // Содержимое формы модерации карточки:
          s.nodeInfoC { mdrSidePanelR.apply },
        ),

        // Визуальный рендер узла:
        s.nodeRenderC { nodeRenderR.apply },

        // Диалог отказа в размещении:
        s.diaRefuseC { mdrDiaRefuseR.apply },

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(

        jdCssC = mrootProxy.connect(_.node.jdCss)( JdCss.JdCssFastEq ),

        nodeInfoC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.node.info) yield {
            for (req <- nextResp.nodeOpt) yield {
              mdrSidePanelR.PropsVal(
                nodeId                  = req.nodeId,
                ntypeOpt = req.ad
                  .map(_ => MNodeTypes.Ad)
                  .orElse {
                    req.nodesMap
                      .get(req.nodeId)
                      .map(_.ntype)
                  },
                nodesMap                = req.nodesMap,
                directSelfNodesSorted   = req.directSelfNodesSorted,
                itemsByType             = req.itemsByType,
                mdrPots                 = mroot.node.mdrPots,
                withTopOffset           = mroot.conf.onNodeKey.nonEmpty,
                currentRcvrId           = mroot.conf.rcvrIdOpt,
              )
            }
          }
        }( DiodeUtil.FastEqExt.PotFastEq( OptFastEq.Wrapped(mdrSidePanelR.NodeMdrRPropsValFastEq)) ),

        nodeRenderC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.node.info) yield {
            for (req <- nextResp.nodeOpt) yield {
              nodeRenderR.PropsVal(
                adData      = req.ad,
                jdCss       = mroot.node.jdCss,
                adnNodeOpt  = req.mdrNodeOpt,
                isSu        = mroot.conf.isSu
              )
            }
          }
        }( DiodeUtil.FastEqExt.PotAsOptionFastEq( OptFastEq.Wrapped(nodeRenderR.NodeRenderRPropsValFastEq) ) ),

        mdrErrorsC = mrootProxy.connect { mroot =>
          for {
            req <- mroot.node.info.toOption
            if req.errorNodeIds.nonEmpty
          } yield {
            mdrErrorsR.PropsVal(
              errorNodeIds  = req.errorNodeIds,
              isSu          = mroot.conf.isSu,
              fixNodesPots  = mroot.node.fixNodePots
            )
          }
        }( OptFastEq.Wrapped( mdrErrorsR.MdrErrorsRPropsValFastEq ) ),

        diaRefuseC = mrootProxy.connect { mroot =>
          val s = mroot.dialogs.refuse
          mdrDiaRefuseR.PropsVal(
            state      = s,
            dismissReq = s.actionInfo
              .flatMap(mroot.node.mdrPots.get)
              .getOrElse( Pot.empty )
          )
        }( mdrDiaRefuseR.MdrDiaRefuseRPropsValFastEq ),

        mdrToolBarC = mrootProxy.connect { mroot =>
          val nextRespOpt = mroot.node.info.toOption
          val nodeInfoOpt = nextRespOpt.flatMap(_.nodeOpt)

          mdrToolBarR.PropsVal(
            nodePending = mroot.node.info.isPending,
            nodeOffset = mroot.node.nodeOffset,
            nodeIdOpt = nodeInfoOpt.map(_.nodeId),
            ntypeOpt = nodeInfoOpt
              .flatMap(_.ad)
              .map(_ => MNodeTypes.Ad)
              .orElse {
                for {
                  nodeInfo <- nodeInfoOpt
                  adnNode  <- nodeInfo.nodesMap.get( nodeInfo.nodeId )
                } yield {
                  adnNode.ntype
                }
              },
            queueReportOpt = nextRespOpt.map(_.mdrQueue),
            errorsCount = nextRespOpt.fold(0)(_.errorNodeIds.size)
          )
        }( mdrToolBarR.MdrControlPanelRPropsValFastEq ),

        mdrForceAllNodesC = mrootProxy.connect { mroot =>
          for (_ <- mroot.conf.rcvrIdOpt) yield {
            mdrForceAllNodesR.PropsVal(
              checked  = mroot.form.forceAllRcrvs,
              disabled = mroot.node.info.isPending
            )
          }
        }( OptFastEq.OptValueEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply( mrootProxy: Props ) = component( mrootProxy )

}
