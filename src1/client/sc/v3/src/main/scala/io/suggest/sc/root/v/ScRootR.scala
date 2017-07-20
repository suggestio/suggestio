package io.suggest.sc.root.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.hdr.v.HeaderR
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.inx.v.IndexR
import io.suggest.sc.root.m.MScRoot
import io.suggest.sc.styl.ScCss.scCss
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
object ScRootR {

  import io.suggest.sjs.common.spa.OptFastEq.Plain
  import MScIndex.MScIndexFastEq

  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    colorsOptC     : ReactConnectProxy[ScCssR.PropsVal],
                                    indexPropsC    : ReactConnectProxy[MScIndex]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      <.div(
        // Рендер стилей перед снаружи и перед остальной выдачей.
        s.colorsOptC { ScCssR.apply },

        <.div(
          // Ссылаемся на стиль.
          scCss.Root.root,


          // Компонент index'а выдачи:
          s.indexPropsC { IndexR.apply }

          // TODO Focused
          // TODO Grid
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Root")
    .initialStateFromProps { propsProxy =>
      State(
        colorsOptC  = propsProxy.connect { props =>
          props.index
            .resp
            .toOption
            .map(_.colors)
        },
        indexPropsC = propsProxy.connect(_.index)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
