package io.suggest.sys.mdr.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.spa.DiodeUtil
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
                   val nodeMdrR: NodeMdrR
                 ) {

  type Props_t = MSysMdrRootS
  type Props = ModelProxy[Props_t]


  case class State(
                    nodeInfoC     : ReactConnectProxy[nodeMdrR.Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Содержимое формы модерации карточки:
        s.nodeInfoC { nodeMdrR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(

        nodeInfoC = mrootProxy.connect { mroot =>
          for (req <- mroot.info) yield {
            nodeMdrR.PropsVal(
              nodesMap                = req.nodesMap,
              directSelfNodesSorted   = req.directSelfNodesSorted,
              itemsByType             = req.itemsByType,
            )
          }
        }( DiodeUtil.FastEqExt.PotAsOptionFastEq( nodeMdrR.NodeMdrRPropsValFastEq ) )

      )
    }
    .renderBackend[Backend]
    .build

  def apply( mrootProxy: Props ) = component( mrootProxy )

}
