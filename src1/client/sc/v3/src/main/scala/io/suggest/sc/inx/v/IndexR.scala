package io.suggest.sc.inx.v

import diode.react.{ModelProxy, ReactConnectProxy}
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

  type Props = ModelProxy[MScIndex]

  protected[this] case class State(
                                    wcPropsOptC: ReactConnectProxy[Option[WelcomeR.PropsVal]]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(
        s.wcPropsOptC { WelcomeR.apply }
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Index")
    .initialStateFromProps { propsProxy =>
      State(
        wcPropsOptC = propsProxy.connect { props =>
          for {
            resp    <- props.resp.toOption
            wcInfo  <- resp.welcome
            isWcHiding <- props.state.welcomeState
          } yield {
            WelcomeR.PropsVal(
              wcInfo   = wcInfo,
              screen   = props.state.screen,
              nodeName = resp.name,
              state    = Some(isWcHiding)
            )
          }
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scIndexProxy: Props) = component( scIndexProxy )

}
