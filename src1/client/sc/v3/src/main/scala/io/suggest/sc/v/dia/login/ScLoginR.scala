package io.suggest.sc.v.dia.login

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.dia.MScLoginS
import ReactCommonUtil.Implicits._
import io.suggest.id.login.v.LoginFormR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.08.2020 16:16
  * Description: Компонент для диалога формы логина, которая живёт в отдельном модуле.
  */
final class ScLoginR(
                      loginFormR: LoginFormR,
                    ) {

  type Props = ModelProxy[MScLoginS]

  case class State(
                    isVisibleSomeC    : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      s.isVisibleSomeC { isVisibleSomeProxy =>
        val isVisible = isVisibleSomeProxy.value.value
        ReactCommonUtil.maybeEl( isVisible ) {
          p.value.circuit.whenDefinedEl { loginFormCircuit =>
            loginFormCircuit.wrap( identity(_) )( loginFormR.component.apply )
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleSomeC = propsProxy.connect { m =>
          OptionUtil.SomeBool( m.isDiaOpened )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
