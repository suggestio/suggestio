package io.suggest.ad.edit.v.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.ad.edit.m.pop.MPictureCropPopup
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.pop.PopupsContR
import io.suggest.lk.r.ErrorPopupR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 16:27
  * Description: Lk Ad Edit Popups -- все попапы формы живут здесь.
  */
class LaePopupsR(
                  val pictureCropPopupR: PictureCropPopupR
                ) {

  import MPictureCropPopup.MPictureCropPopupFastEq

  type Props = ModelProxy[MAeRoot]

  protected case class State(
                              popupsContPropsC    : ReactConnectProxy[PopupsContR.PropsVal],
                              errorMsgsC          : ReactConnectProxy[Option[MErrorPopupS]],
                              cropPopPropsOptC    : ReactConnectProxy[Option[MPictureCropPopup]]
                            )

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement = {
      s.popupsContPropsC { popupContProps =>
        PopupsContR(popupContProps)(

          // Попап с ~отрендеренными ошибками:
          s.errorMsgsC { ErrorPopupR.apply },

          // Попап кропа картинки:
          s.cropPopPropsOptC { pictureCropPopupR.apply }

        )
      }
    }
  }


  val component = ScalaComponent.builder[Props]("LaePops")
    .initialStateFromProps { rootProxy =>
      State(

        popupsContPropsC = rootProxy.connect { mroot =>
          PopupsContR.PropsVal(
            visible = mroot.popups.nonEmpty
          )
        },

        errorMsgsC = rootProxy.connect { _.popups.error },

        cropPopPropsOptC = rootProxy.connect { _.popups.pictureCrop }

      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component( rootProxy )

}
