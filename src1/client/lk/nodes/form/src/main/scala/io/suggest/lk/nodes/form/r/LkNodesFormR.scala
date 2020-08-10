package io.suggest.lk.nodes.form.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.nodes.form.m.MLkNodesRoot
import io.suggest.lk.nodes.form.r.tree.TreeR
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._

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

  /** Состояние содержит коннекшены до под-моделей. */
  case class State(
                    treeC: ReactConnectProxy[MLkNodesRoot],
                  )


  /** Вся суть react-компонента формы обитает здесь. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        // Рендер текущего дерева узлов
        s.treeC( treeR.component.apply )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        treeC = p.connect( identity(_) )(
          FastEqUtil[MLkNodesRoot] { (a, b) =>
            (a.tree ===* b.tree) &&
            (a.conf ===* b.conf)
          }
        )
      )
    }
    .renderBackend[Backend]
    .build

}
