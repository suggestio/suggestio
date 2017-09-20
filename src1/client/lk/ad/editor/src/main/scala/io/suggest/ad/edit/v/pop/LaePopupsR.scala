package io.suggest.ad.edit.v.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.i18n.IMessage
import io.suggest.lk.pop.PopupsContR
import io.suggest.lk.r.ErrorMsgsPopupR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 16:27
  * Description: Lk Ad Edit Popups -- все попапы формы живут здесь.
  */
class LaePopupsR {

  import PopupsContR.PopContPropsValFastEq

  type Props = ModelProxy[MAeRoot]

  protected case class State(
                              popupsContPropsC    : ReactConnectProxy[PopupsContR.PropsVal],
                              errorMsgsC          : ReactConnectProxy[Seq[IMessage]]
                            )

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement = {
      s.popupsContPropsC { popupContProps =>
        PopupsContR(popupContProps)(

          // Попап с ~отрендеренными ошибками:
          s.errorMsgsC { ErrorMsgsPopupR.apply }

          // TODO Попап кропа картинки.

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props]("LaePops")
    .initialStateFromProps { rootProxy =>
      State(
        popupsContPropsC = rootProxy.connect { mroot =>
          PopupsContR.PropsVal(
            visible = mroot.doc.errors.nonEmpty
          )
        },
        errorMsgsC = rootProxy.connect { mroot =>
          mroot.doc.errors
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component( rootProxy )

}
