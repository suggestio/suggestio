package io.suggest.sc.root.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.grid.v.GridR
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.inx.v.IndexR
import io.suggest.sc.root.m.MScRoot
import io.suggest.sc.styl.{GetScCssF, MScCssArgs}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                indexR                      : IndexR,
                protected[this] val scCssR  : ScCssR,
                val gridR                   : GridR,
                getScCssF                   : GetScCssF
              ) {

  import MScCssArgs.MScCssArgsFastEq
  import MScIndex.MScIndexFastEq
  import gridR.GridPropsValFastEq

  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    scCssArgsC     : ReactConnectProxy[MScCssArgs],
                                    indexPropsC    : ReactConnectProxy[MScIndex],
                                    gridPropsOptC  : ReactConnectProxy[gridR.PropsVal]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      val scCss = getScCssF()
      <.div(
        // Рендер стилей перед снаружи и перед остальной выдачей.
        s.scCssArgsC { scCssR.apply },

        <.div(
          // Ссылаемся на стиль.
          scCss.Root.root,

          // Компонент index'а выдачи:
          s.indexPropsC { indexR.apply },

          // Рендер плитки карточек узла:
          s.gridPropsOptC { gridR.apply }

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Root")
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.scCssArgs),
        indexPropsC = propsProxy.connect(_.index),
        gridPropsOptC = propsProxy.connect { mroot =>
          gridR.PropsVal(
            grid    = mroot.grid,
            fgColor = mroot.index.resp.toOption.flatMap(_.colors.fg)
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
