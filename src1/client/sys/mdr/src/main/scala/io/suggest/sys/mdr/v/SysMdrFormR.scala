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
import io.suggest.sys.mdr.v.toolbar.MdrToolBarR
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
                   val mdrControlPanelR   : MdrToolBarR,
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
                    controlPanelC       : ReactConnectProxy[mdrControlPanelR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      // Панель управления модерации.
      <.div(

        // Рендер css карточки:
        s.jdCssC { jdCssR.apply },

        // Тулбар, без AppBar, т.к. он неуместен и делает неконтрастный фон.
        MuiToolBar()(
          s.controlPanelC { mdrControlPanelR.apply }
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

        jdCssC = mrootProxy.connect(_.jdCss)( JdCss.JdCssFastEq ),

        nodeInfoC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.info) yield {
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
                mdrPots                 = mroot.mdrPots,
                withTopOffset           = mroot.conf.onNodeKey.nonEmpty,
              )
            }
          }
        }( DiodeUtil.FastEqExt.PotFastEq( OptFastEq.Wrapped(mdrSidePanelR.NodeMdrRPropsValFastEq)) ),

        nodeRenderC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.info) yield {
            for (req <- nextResp.nodeOpt) yield {
              nodeRenderR.PropsVal(
                adData      = req.ad,
                jdCss       = mroot.jdCss,
                adnNodeOpt  = req.mdrNodeOpt,
                isSu        = mroot.conf.isSu
              )
            }
          }
        }( DiodeUtil.FastEqExt.PotAsOptionFastEq( OptFastEq.Wrapped(nodeRenderR.NodeRenderRPropsValFastEq) ) ),

        mdrErrorsC = mrootProxy.connect { mroot =>
          for {
            req <- mroot.info.toOption
            if req.errorNodeIds.nonEmpty
          } yield {
            mdrErrorsR.PropsVal(
              errorNodeIds  = req.errorNodeIds,
              isSu          = mroot.conf.isSu,
            )
          }
        }( OptFastEq.Wrapped( mdrErrorsR.MdrErrorsRPropsValFastEq ) ),

        diaRefuseC = mrootProxy.connect { mroot =>
          val s = mroot.dialogs.refuse
          mdrDiaRefuseR.PropsVal(
            state      = s,
            dismissReq = s.actionInfo
              .flatMap(mroot.mdrPots.get)
              .getOrElse( Pot.empty )
          )
        }( mdrDiaRefuseR.MdrDiaRefuseRPropsValFastEq ),

        controlPanelC = mrootProxy.connect { mroot =>
          val nextRespOpt = mroot.info.toOption
          val nodeInfoOpt = nextRespOpt.flatMap(_.nodeOpt)

          mdrControlPanelR.PropsVal(
            nodePending = mroot.info.isPending,
            nodeOffset = mroot.nodeOffset,
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
        }( mdrControlPanelR.MdrControlPanelRPropsValFastEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply( mrootProxy: Props ) = component( mrootProxy )

}
