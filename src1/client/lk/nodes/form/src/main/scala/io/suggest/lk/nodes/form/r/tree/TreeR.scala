package io.suggest.lk.nodes.form.r.tree

import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m.{MNodeState, MTree, NodeNameClick}
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
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

    /** Callback реакции на клик по заголовку узла. */
    def onNodeClick(rcvrKey: RcvrKey): Callback = {
      $.props >>= { p =>
        p.dispatchCB( NodeNameClick(rcvrKey) )
      }
    }


    /**
      * Рекурсивный рендер под-дерева узлов.
      *
      * @param node Корневой узел этого под-дерева.
      * @param parentRcvrKey Ключ родительского узла [Nil].
      * @param level Уровень [0].
      * @return React-элемент.
      */
    def _renderNode(node: MNodeState, parentRcvrKey: RcvrKey, level: Int): ReactElement = {
      val rcvrKey = node.info.id :: parentRcvrKey
      // Контейнер узла узла + дочерних узлов.
      <.div(
        ^.key := node.info.id,

        (level > 0) ?= {
          ^.marginLeft := (level * 10).px
        },

        // контейнер названия текущего узла
        <.div(
          ^.onClick --> onNodeClick(rcvrKey),
          node.info.name
        ),

        // Если инфа по узлу запрашивается с сервера, от отрендерить прелоадер
        node.children.renderPending { _ =>
          val pleaseWait = Messages("Please.wait")
          LkPreLoader.PRELOADER_IMG_URL.fold [ReactElement] {
            <.span( pleaseWait )
          } { url =>
            <.img(
              ^.src := url,
              ^.alt := pleaseWait
            )
          }
        },

        // Рекурсивно отрендерить дочерние элементы:
        node.children.render { children =>
          <.div(
            children.nonEmpty ?= {
              val childLevel = level + 1
              for (subNode <- children) yield {
                _renderNode(subNode, rcvrKey, childLevel)
              }
            }
          )
        },

        // При ошибке запроса отрендерить тут что-то про ошибку.
        node.children.renderFailed { ex =>
          <.span(
            ^.title   := ex.toString,
            ^.`class` := Css.Colors.RED,
            Messages("Error")
          )
        }

      )
    }


    /** Рендер текущего компонента. */
    def render(p: Props): ReactElement = {
      val v = p()
      val parentLevel = 0
      val parentRcvrKey = Nil

      <.div(
        // Рендерить узлы.
        for (node <- v.nodes) yield {
          _renderNode(node, parentRcvrKey, parentLevel)
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
