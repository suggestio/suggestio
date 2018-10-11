package io.suggest.sys.mdr.v

import chandu0101.scalajs.react.components.materialui.{MuiDivider, MuiDraweAnchors, MuiDrawer, MuiDrawerProps, MuiDrawerVariants}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.jd.render.v.{JdCss, JdCssR}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.spa.{DiodeUtil, OptFastEq}
import io.suggest.sys.mdr.m.MSysMdrRootS
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
                   val nodeMdrR       : NodeMdrR,
                   val nodeRenderR    : NodeRenderR,
                   val mdrErrorsR     : MdrErrorsR,
                   val mdrDiaRefuseR  : MdrDiaRefuseR,
                   jdCssR             : JdCssR,
                 ) {

  import JdCss.JdCssFastEq

  type Props_t = MSysMdrRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    jdCssC              : ReactConnectProxy[JdCss],
                    nodeInfoC           : ReactConnectProxy[nodeMdrR.Props_t],
                    nodeRenderC         : ReactConnectProxy[nodeRenderR.Props_t],
                    mdrErrorsC          : ReactConnectProxy[mdrErrorsR.Props_t],
                    diaRefuseC          : ReactConnectProxy[mdrDiaRefuseR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Рендер css карточки:
        s.jdCssC { jdCssR.apply },

        // Ошибки - здесь:
        s.mdrErrorsC { mdrErrorsR.apply },
        <.br,

        // Левая панель:
        MuiDrawer(
          new MuiDrawerProps {
            override val variant = MuiDrawerVariants.permanent
            override val anchor = MuiDraweAnchors.right
          }
        )(
          // Содержимое формы модерации карточки:
          s.nodeInfoC { nodeMdrR.apply },

          MuiDivider(),
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

        jdCssC = mrootProxy.connect(_.jdCss),

        nodeInfoC = mrootProxy.connect { mroot =>
          for (reqOpt <- mroot.info) yield {
            nodeMdrR.PropsVal(
              nodeOpt = for (req <- reqOpt) yield {
                nodeMdrR.NodePropsVal(
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
                )
              },
              nodeOffset = mroot.nodeOffset - mroot.info.toOption.flatten.fold(0)(_.errorNodeIds.size)
            )
          }
        }( DiodeUtil.FastEqExt.PotFastEq( nodeMdrR.NodeMdrRPropsValFastEq) ),

        nodeRenderC = mrootProxy.connect { mroot =>
          for {
            reqOpt <- mroot.info
            if reqOpt.nonEmpty
            req = reqOpt.get
          } yield {
            nodeRenderR.PropsVal(
              adData      = req.ad,
              jdCss       = mroot.jdCss,
              adnNodeOpt  = req.mdrNodeOpt
            )
          }
        }( DiodeUtil.FastEqExt.PotAsOptionFastEq( nodeRenderR.NodeRenderRPropsValFastEq ) ),

        mdrErrorsC = mrootProxy.connect { mroot =>
          for {
            req <- mroot.info.toOption.flatten
            if req.errorNodeIds.nonEmpty
          } yield {
            mdrErrorsR.PropsVal(
              errorNodeIds = req.errorNodeIds
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
        }( mdrDiaRefuseR.MdrDiaRefuseRPropsValFastEq )

      )
    }
    .renderBackend[Backend]
    .build

  def apply( mrootProxy: Props ) = component( mrootProxy )

}
