package io.suggest.adn.edit.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.m.{MAdnEditForm, MLkAdnEditRoot, Save}
import io.suggest.lk.r.SaveR
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CallbackTo, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 18:57
  * Description: React-компонент аддона к правой панели.
  * Изначально содержал только кнопку "Сохранить" с сопутствующим обвесом.
  */
class RightBarR(
                 saveR: SaveR
               ) {

  type Props_t = MLkAdnEditRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    savePotC      : ReactConnectProxy[Pot[MAdnEditForm]]
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      <.div(
        s.savePotC { reqPotProxy =>
          saveR.component(
            reqPotProxy.zoom { reqPot =>
              saveR.PropsVal(
                currentReq = reqPot,
                onClick = CallbackTo.pure( Save ),
              )
            }
          )
        },
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(
        savePotC = mrootProxy.connect { mroot =>
          mroot.internals.saving
        }
      )
    }
    .renderBackend[Backend]
    .build

}
