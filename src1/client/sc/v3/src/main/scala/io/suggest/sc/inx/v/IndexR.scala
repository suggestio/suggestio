package io.suggest.sc.inx.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.hdr.m.MHeaderStates
import io.suggest.sc.hdr.v.HeaderR
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.inx.v.wc.WelcomeR
import io.suggest.sc.search.m.MScSearch
import io.suggest.sc.search.v.SearchR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 22:49
  * Description: React-компонент индекса выдачи.
  */
object IndexR {

  import HeaderR.HeaderPropsValFastEq
  import MScSearch.MScSearchFastEq


  type Props = ModelProxy[MScIndex]

  protected[this] case class State(
                                    headerPropsC  : ReactConnectProxy[Option[HeaderR.PropsVal]],
                                    wcPropsOptC   : ReactConnectProxy[Option[WelcomeR.PropsVal]],
                                    searchC       : ReactConnectProxy[MScSearch]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      <.div(

        // Экран приветствия узла:
        s.wcPropsOptC { WelcomeR.apply },

        // Компонент заголовка выдачи:
        s.headerPropsC { HeaderR.apply },

        // TODO Правая панель.
        s.searchC { SearchR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Index")
    .initialStateFromProps { propsProxy =>
      State(
        headerPropsC = propsProxy.connect { props =>
          for {
            resp <- props.resp.toOption
          } yield {
            HeaderR.PropsVal(
              // TODO Определять маркер состояния на основе состояния полей в props.
              hdrState  = if (props.search.isShown) {
                MHeaderStates.Search
              } else {
                MHeaderStates.PlainGrid
              },
              node      = resp
            )
          }
        },
        wcPropsOptC = propsProxy.connect { props =>
          for {
            resp    <- props.resp.toOption
            wcInfo  <- resp.welcome
            wcState <- props.welcome
          } yield {
            WelcomeR.PropsVal(
              wcInfo   = wcInfo,
              screen   = props.state.screen,
              nodeName = resp.name,
              state    = wcState
            )
          }
        },
        searchC = propsProxy.connect(_.search)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scIndexProxy: Props) = component( scIndexProxy )

}
