package io.suggest.lk.nodes.form.r.tree

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.m._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:19
  * Description: React-компонент дерева узлов.
  * Каждый узел дерева описывается компонентом [[NodeR]].
  */
object TreeR {

  import MNodeMenuS.MNodeMenuSFastEq


  type Props = ModelProxy[PropsVal]

  /** Внутреннее состояние компонента дерева содержит коннекшены для внутренних компонентов. */
  protected case class State(
                              nodeMenuOptProxy    : ReactConnectProxy[Option[MNodeMenuS]]
                            )


  implicit object TreeRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.conf eq b.conf) &&
        (a.mtree eq b.mtree)
    }
  }
  case class PropsVal(
                       conf         : MLknConf,
                       mtree        : MTree,
                       nodeMenuOpt  : Option[MNodeMenuS]
                     )


  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props, State]) {

    /** Рендер текущего компонента. */
    def render(p: Props, s: State): ReactElement = {
      val v = p()
      val parentLevel = 0
      val parentRcvrKey = Nil

      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        for (node <- v.mtree.nodes) yield {
          val tnp = NodeR.PropsVal(
            conf          = v.conf,
            mtree         = v.mtree,
            node          = node,
            parentRcvrKey = parentRcvrKey,
            level         = parentLevel,
            proxy         = p,
            menuOptC      = s.nodeMenuOptProxy
          )
          NodeR( tnp )
        }
      )
    }

  }


  val component = ReactComponentB[Props]("Tree")
    .initialState_P { p =>
      State(
        nodeMenuOptProxy = p.connect(_.nodeMenuOpt)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mtreeP: Props) = component(mtreeP)

}
