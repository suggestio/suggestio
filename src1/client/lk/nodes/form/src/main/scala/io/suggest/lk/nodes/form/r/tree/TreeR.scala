package io.suggest.lk.nodes.form.r.tree

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:19
  * Description: React-компонент дерева узлов.
  * Каждый узел дерева описывается компонентом [[NodeR]].
  */
class TreeR(
             nodeR: NodeR
           ) {

  type Props = ModelProxy[MLkNodesRoot]


  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props, Unit]) {

    /** Рендер текущего компонента. */
    def render(p: Props): VdomElement = {
      val v = p()
      val parentLevel = 0
      val parentRcvrKey = Nil

      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        v.tree.nodes.toVdomArray { node =>
          val tnp = nodeR.PropsVal(
            conf          = v.conf,
            mtree         = v.tree,
            node          = node,
            parentRcvrKey = parentRcvrKey,
            level         = parentLevel,
            proxy         = p,
          )
          nodeR.component.withKey(node.info.id)( tnp )
        }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
