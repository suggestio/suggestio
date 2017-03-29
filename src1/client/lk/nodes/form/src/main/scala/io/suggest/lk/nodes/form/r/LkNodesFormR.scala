package io.suggest.lk.nodes.form.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.nodes.form.m.MLkNodesRoot
import io.suggest.lk.nodes.form.r.tree.TreeR
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 17:39
  * Description: React-компонент формы управления узлами.
  */
object LkNodesFormR {

  import TreeR.TreeRPropsValFastEq


  type Props = ModelProxy[MLkNodesRoot]

  /** Состояние содержит коннекшены до под-моделей. */
  case class State(
                    treeC: ReactConnectProxy[TreeR.PropsVal]
                  )


  /** Вся суть react-компонента формы обитает здесь. */
  class Backend($: BackendScope[Props, State]) {

    def render(s: State): ReactElement = {
      <.div(
        // Рендер текущего дерева узлов
        s.treeC { TreeR.apply }
      )
    }

  }


  val component = ReactComponentB[Props]("LkNodesForm")
    .initialState_P { p =>
      State(
        treeC = p.connect { v =>
          TreeR.PropsVal(
            conf        = v.conf,
            mtree       = v.tree,
            nodeMenuOpt = v.nodeMenu
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
