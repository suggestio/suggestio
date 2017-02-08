package io.suggest.lk.nodes.form.r.tree

import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.MTree
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:19
  * Description: React-компонент дерева узлов.
  */
object TreeR {

  type Props = ModelProxy[MTree]


  class Backend($: BackendScope[Props,_]) {

    def render(p: Props): ReactElement = {
      <.div(
        // Рендерить узлы. TODO Рендерить рекурсивно со сдвигом.
        for (node <- p().nodes) yield {
          <.div(
            ^.key := node.id,
            node.name
          )
        }
      )
    }

  }


  val component = ReactComponentB[Props]("Tree")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mtreeP: Props) = component(mtreeP)

}
