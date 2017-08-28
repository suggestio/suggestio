package io.suggest.ad.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAdEditRoot
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs}
import io.suggest.jd.render.v.{JdCssR, JdR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:56
  * Description: React-компонент всей формы react-редактора карточек.
  */
class LkAdEditFormR(
                     jdCssR   : JdCssR,
                     jdR      : JdR
                   ) {

  import MJdArgs.MJdWithArgsFastEq

  type Props = ModelProxy[MAdEditRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC : ReactConnectProxy[MJdArgs],
                              jdCssArgsC     : ReactConnectProxy[MJdCssArgs]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(

        // Рендер css
        s.jdCssArgsC { jdCssR.apply },

        // Рендер preview
        s.jdPreviewArgsC { jdR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("AdEd")
    .initialStateFromProps { p =>
      State(
        jdPreviewArgsC = p.connect { mroot =>
          mroot.doc.jdArgs
        },
        jdCssArgsC = p.connect { mroot =>
          mroot.doc.jdArgs.singleCssArgs
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
