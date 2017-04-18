package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.{MLamS, MRoot}
import io.suggest.maps.m.MMapS
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import MMapS.MMapSFastEq
import MLamS.MLamSFastEq
import io.suggest.css.Css

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:31
  * Description: top-level компонент для react-экранизации формы размещения узла на карте.
  *
  * Все react-компоненты были наготово взяты из lk-adv-geo-формы, как и в прошлый раз
  * на fsm-mvm архитектуре.
  */
object LkAdnMapFormR {

  type Props = ModelProxy[MRoot]


  protected[this] case class State(
                                    mmapC            : ReactConnectProxy[MMapS],
                                    lamC             : ReactConnectProxy[MLamS]
                                  )


  /** Логика рендера компонента всея формы. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): ReactElement = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        "TODO"
      )
    }

  }


  val component = ReactComponentB[Props]("LamForm")
    .initialState_P { p =>
      State(
        mmapC = p.connect(_.map),
        lamC  = p.connect(_.lam)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mRootProxy: Props) = component(mRootProxy)

}
