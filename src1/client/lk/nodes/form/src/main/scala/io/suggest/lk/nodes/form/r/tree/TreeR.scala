package io.suggest.lk.nodes.form.r.tree

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.nodes.MLknConf
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
object TreeR {

  type Props = ModelProxy[PropsVal]


  implicit object TreeRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.conf eq b.conf) &&
        (a.mtree eq b.mtree)
    }
  }
  case class PropsVal(
                       conf         : MLknConf,
                       mtree        : MTree
                     )


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
        v.mtree.nodes.toVdomArray { node =>
          val tnp = NodeR.PropsVal(
            conf          = v.conf,
            mtree         = v.mtree,
            node          = node,
            parentRcvrKey = parentRcvrKey,
            level         = parentLevel,
            proxy         = p
          )
          NodeR( tnp )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Tree")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mtreeP: Props) = component(mtreeP)

}
