package io.suggest.sys.mdr.v

import diode.react.{ModelProxy, ReactConnectProxy}
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
                 ) {

  type Props_t = MSysMdrRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    nodeInfoC           : ReactConnectProxy[nodeMdrR.Props_t],
                    nodeRenderC         : ReactConnectProxy[nodeRenderR.Props_t],
                    mdrErrorsC          : ReactConnectProxy[mdrErrorsR.Props_t],
                    diaRefuseC          : ReactConnectProxy[mdrDiaRefuseR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Ошибки - здесь:
        s.mdrErrorsC { mdrErrorsR.apply },

        // Содержимое формы модерации карточки:
        s.nodeInfoC { nodeMdrR.apply },

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

        nodeInfoC = mrootProxy.connect { mroot =>
          for {
            reqOpt <- mroot.info
          } yield {
            for (req <- reqOpt) yield {
              nodeMdrR.PropsVal(
                nodesMap                = req.nodesMap,
                directSelfNodesSorted   = req.directSelfNodesSorted,
                itemsByType             = req.itemsByType,
              )
            }
          }
        }( DiodeUtil.FastEqExt.PotAsOptionFastEq( OptFastEq.Wrapped(nodeMdrR.NodeMdrRPropsValFastEq) ) ),

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
          for {
            refuseState <- mroot.dialogs.refuse
          } yield {
            mdrDiaRefuseR.PropsVal(
              state = refuseState
            )
          }
        }( OptFastEq.Wrapped(mdrDiaRefuseR.MdrDiaRefuseRPropsValFastEq) )

      )
    }
    .renderBackend[Backend]
    .build

  def apply( mrootProxy: Props ) = component( mrootProxy )

}
