package io.suggest.lk.nodes.form.r.tree

import diode.react.ModelProxy
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.ILknTreeNode
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


  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props,_]) {

    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param node Корневой узел этого под-дерева.
      * @param parentRcvrKey Ключ родительского узла [Nil].
      * @param level Уровень [0].
      * @return React-элемент.
      */
    def _renderNode(node: ILknTreeNode, parentRcvrKey: RcvrKey = Nil, level: Int = 0): ReactElement = {
      val rcvrKey = node.id :: parentRcvrKey
      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.id,

        (level > 0) ?= {
          ^.marginLeft := (level * 10).px
        },

        // контейнер текущего узла
        <.div(
          node.name
        ),

        node.children.nonEmpty ?= {
          val childLevel = level + 1
          for (subNode <- node.children) yield {
            _renderNode(subNode, rcvrKey, childLevel)
          }
        }
      )
    }

    /** Рендер текущего компонента. */
    def render(p: Props): ReactElement = {
      val v = p()
      <.div(
        // Рендерить узлы.
        for (node <- v.nodes) yield {
          _renderNode(node)
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
