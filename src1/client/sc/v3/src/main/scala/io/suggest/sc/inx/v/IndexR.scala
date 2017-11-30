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
import io.suggest.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 22:49
  * Description: React-компонент индекса выдачи.
  */
class IndexR(
              searchR                         : SearchR,
              protected[this] val headerR     : HeaderR,
              protected[this] val welcomeR    : WelcomeR
            ) {

  import headerR.HeaderPropsValFastEq
  import MScSearch.MScSearchFastEq


  type Props = ModelProxy[MScIndex]

  protected[this] case class State(
                                    headerPropsC  : ReactConnectProxy[Option[headerR.PropsVal]],
                                    wcPropsOptC   : ReactConnectProxy[Option[welcomeR.PropsVal]]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(p: Props, s: State): VdomElement = {
      <.div(

        // Экран приветствия узла:
        s.wcPropsOptC { welcomeR.apply },

        // Компонент заголовка выдачи:
        s.headerPropsC { headerR.apply },

        // Правая панель (поиск)
        p.wrap(_.search) { searchR.apply }

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
            headerR.PropsVal(
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
            welcomeR.PropsVal(
              wcInfo   = wcInfo,
              nodeName = resp.name,
              state    = wcState
            )
          }
        }

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scIndexProxy: Props) = component( scIndexProxy )

}
