package io.suggest.sc.root.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.hdr.m.MHeaderStates
import io.suggest.sc.hdr.v.HeaderR
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

  import HeaderR.HeaderPropsValFastEq
  import io.suggest.sjs.common.spa.OptFastEq.Plain

  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    colorsOptC     : ReactConnectProxy[ScCssR.PropsVal],
                                    headerProps    : ReactConnectProxy[HeaderR.PropsVal]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      <.div(
        // Рендер стилей перед снаружи и перед остальной выдачей.
        s.colorsOptC { ScCssR.apply },

        <.div(
          // Ссылаемся на стиль.
          scCss.Root.root,

          // Компонент заголовка выдачи:
          s.headerProps { HeaderR.apply }

          // TODO Focused
          // TODO Grid
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Root")
    .initialStateFromProps { propsProxy =>
      State(
        colorsOptC  = propsProxy.connect( _.currNode.map(_.colors) ),
        headerProps = propsProxy.connect { props =>
          HeaderR.PropsVal(
            hdrState = MHeaderStates.PlainGrid,   // TODO Определять маркер состояния на основе состояния полей в props.
            node     = props.currNode
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
