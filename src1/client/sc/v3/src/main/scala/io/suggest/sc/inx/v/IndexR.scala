package io.suggest.sc.inx.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.hdr.m.MHeaderStates
import io.suggest.sc.hdr.v.HeaderR
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.inx.v.wc.WelcomeR
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


  type Props = ModelProxy[MScIndex]

  protected[this] case class State(
                                    headerPropsC  : ReactConnectProxy[Option[HeaderR.PropsVal]],
                                    wcPropsOptC   : ReactConnectProxy[Option[WelcomeR.PropsVal]]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        // Экран приветствия узла:
        s.wcPropsOptC { WelcomeR.apply },

        // Компонент заголовка выдачи:
        s.headerPropsC { HeaderR.apply }

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
              hdrState  = MHeaderStates.PlainGrid, // TODO Определять маркер состояния на основе состояния полей в props.
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
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scIndexProxy: Props) = component( scIndexProxy )

}
