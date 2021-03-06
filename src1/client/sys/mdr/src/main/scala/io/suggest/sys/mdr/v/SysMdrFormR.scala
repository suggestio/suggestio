package io.suggest.sys.mdr.v

import com.materialui.{MuiDrawer, MuiDrawerAnchor, MuiDrawerProps, MuiDrawerVariant, MuiToolBar}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.CssR
import io.suggest.jd.render.v.{JdCss, JdCssStatic}
import io.suggest.lk.u.MaterialUiUtil
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.spa.{FastEqUtil, OptFastEq}
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
                   jdCssStatic            : JdCssStatic,
                 ) {

  type Props_t = MSysMdrRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    jdCssOptC           : ReactConnectProxy[Option[JdCss]],
                    nodeInfoC           : ReactConnectProxy[mdrSidePanelR.Props_t],
                    nodeRenderC         : ReactConnectProxy[nodeRenderR.Props_t],
                    mdrErrorsC          : ReactConnectProxy[mdrErrorsR.Props_t],
                    diaRefuseC          : ReactConnectProxy[mdrDiaRefuseR.Props_t],
                    mdrToolBarC         : ReactConnectProxy[mdrToolBarR.Props_t],
                    mdrForceAllNodesC   : ReactConnectProxy[mdrForceAllNodesR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(p: Props, s: State): VdomElement = {
      // Панель управления модерации.
      val form = <.div(

        // Рендер jd css карточки:
        p.wrap(_ => jdCssStatic)( CssR.compProxied.apply ),
        s.jdCssOptC {
          _ .value
            .whenDefinedEl( CssR.component.apply )
        },

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
            override val variant = MuiDrawerVariant.permanent
            override val anchor  = MuiDrawerAnchor.right
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

      MaterialUiUtil.postprocessTopLevel( form )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(

        jdCssOptC = mrootProxy.connect(_.node.jdArgsOpt.map(_.jdRuntime.jdCss))( OptFastEq.Wrapped(JdCss.JdCssFastEq) ),

        nodeInfoC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.node.info) yield {
            for (req <- nextResp.nodeOpt) yield {
              mdrSidePanelR.PropsVal(
                nodeId                  = req.info.nodeId,
                ntypeOpt = req.ad
                  .map[MNodeType](_ => MNodeTypes.Ad)
                  .orElse {
                    req
                      .nodesMap
                      .get(req.info.nodeId)
                      .flatMap(_.ntype)
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
        }( FastEqUtil.PotFastEq( OptFastEq.Wrapped(mdrSidePanelR.NodeMdrRPropsValFastEq)) ),

        nodeRenderC = mrootProxy.connect { mroot =>
          for (nextResp <- mroot.node.info) yield {
            for (req <- nextResp.nodeOpt) yield {
              nodeRenderR.PropsVal(
                jdArgsOpt   = mroot.node.jdArgsOpt,
                gridBuildRes = mroot.node.gridBuild,
                adnNodeOpt  = req.mdrNodeOpt,
                isSu        = mroot.conf.isSu,
              )
            }
          }
        }( FastEqUtil.PotAsOptionFastEq( OptFastEq.Wrapped(nodeRenderR.NodeRenderRPropsValFastEq) ) ),

        mdrErrorsC = mrootProxy.connect { mroot =>
          for {
            req <- mroot.node.info.toOption
            if req.resp.errorNodeIds.nonEmpty
          } yield {
            mdrErrorsR.PropsVal(
              errorNodeIds  = req.resp.errorNodeIds,
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
            nodeIdOpt = nodeInfoOpt.map(_.info.nodeId),
            ntypeOpt = nodeInfoOpt
              .flatMap(_.ad)
              .map[MNodeType](_ => MNodeTypes.Ad)
              .orElse {
                for {
                  nodeInfo <- nodeInfoOpt
                  adnNode  <- nodeInfo.nodesMap.get( nodeInfo.info.nodeId )
                  ntype    <- adnNode.ntype
                } yield ntype
              },
            queueReportOpt = nextRespOpt.map(_.resp.mdrQueue),
            errorsCount = nextRespOpt.fold(0)(_.resp.errorNodeIds.size)
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
