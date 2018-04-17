package io.suggest.adn.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.m.MLkAdnEditRoot
import io.suggest.lk.r.SaveR
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 18:57
  * Description: React-компонент аддона к правой панели.
  * Изначально содержал только кнопку "Сохранить" с сопутствующим обвесом.
  */
class RightBarR(
                 val saveR: SaveR
               ) {

  type Props_t = MLkAdnEditRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    savePotC      : ReactConnectProxy[saveR.PropsVal]
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      <.div(
        s.savePotC { saveR.apply }
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(
        savePotC = mrootProxy.connect { mroot =>
          saveR.PropsVal(
            currentReq = mroot.internals.saving
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mrootProxy: Props) = component( mrootProxy )

}
