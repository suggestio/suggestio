package io.suggest.lk.nodes.form.r.tree

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m._
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._

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


  case class State(
                    root4nodeC        : ReactConnectProxy[MLkNodesRoot],
                  )

  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props, State]) {

    /** Рендер текущего компонента. */
    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        s.root4nodeC { mrootProxy =>
          val mroot = mrootProxy.value
          <.div(
            mroot.tree.nodes
              .zipWithIndex
              .deepMapFold[NodePath_t, VdomElement]( Nil ) { case (parentNodePathRev, subTree) =>
                val (mns, i) = subTree.rootLabel
                val currNodePathRev: NodePath_t = i :: parentNodePathRev
                val comp = nodeR.component.withKey( currNodePathRev.mkString(".") )(
                  nodeR.PropsVal(
                    node          = mns,
                    nodePathRev   = currNodePathRev,
                    confAdId      = mroot.conf.adIdOpt,
                    opened        = mroot.tree.opened,
                    chCount       = subTree.subForest.length,
                    chCountEnabled = subTree.subForest
                      .iterator
                      .count { chTree =>
                        chTree.rootLabel._1.info.isEnabled
                      },
                    proxy         = p,
                  )
                )
                currNodePathRev -> comp
              }
              .flatten
              .iterator
              .toVdomArray,
          )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        root4nodeC = propsProxy.connect(identity(_))( FastEqUtil[MLkNodesRoot] { (a, b) =>
          (a.tree.nodes ===* b.tree.nodes) &&
          (a.tree.opened ===* b.tree.opened) &&
          (a.conf.adIdOpt ===* b.conf.adIdOpt)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
