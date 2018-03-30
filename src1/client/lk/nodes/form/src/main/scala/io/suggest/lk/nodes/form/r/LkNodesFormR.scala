package io.suggest.lk.nodes.form.r

import diode.react.{ModelProxy, ReactConnectProxy}
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
                    val treeR: TreeR
                  ) {

  import treeR.TreeRPropsValFastEq


  type Props = ModelProxy[MLkNodesRoot]

  /** Состояние содержит коннекшены до под-моделей. */
  case class State(
                    treeC: ReactConnectProxy[treeR.PropsVal]
                  )


  /** Вся суть react-компонента формы обитает здесь. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        // Рендер текущего дерева узлов
        s.treeC { treeR.apply }
      )
    }

  }


  val component = ScalaComponent.builder[Props]("LkNodesForm")
    .initialStateFromProps { p =>
      State(
        treeC = p.connect { v =>
          treeR.PropsVal(
            conf        = v.conf,
            mtree       = v.tree
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
