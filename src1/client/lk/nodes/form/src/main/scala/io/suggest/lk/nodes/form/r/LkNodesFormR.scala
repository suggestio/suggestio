package io.suggest.lk.nodes.form.r

import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.MLkNodesRoot
import io.suggest.lk.nodes.form.r.tree.TreeR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 17:39
  * Description: React-компонент формы управления узлами.
  */
class LkNodesFormR(
                    treeR: TreeR
                  ) {


  type Props = ModelProxy[MLkNodesRoot]


  /** Вся суть react-компонента формы обитает здесь. */
  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      <.div(
        // Рендер текущего дерева узлов
        treeR.component( p ),
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
