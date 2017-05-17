package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adn.map.m.IRadOpts
import io.suggest.maps.m.MRadT
import MRadT.MRadTFastEq
import io.suggest.maps.r.rad.{DraggablePinMarkerR, RadMapControlsR}
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.layer.LayerGroupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 11:49
  * Description: react-компонент маркера узла.
  */
object MapCursorR {

  type Props = ModelProxy[IRadOpts[_]]


  protected[this] case class State(
                                    pointOptC      : ReactConnectProxy[Option[MGeoPoint]],
                                    mRadTOptC      : ReactConnectProxy[Option[MRadT[_]]]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): ReactElement = {
      // Маркер центра круга.
      LayerGroupR()(

        // Опциональное размещение в точке.
        s.pointOptC { DraggablePinMarkerR.apply },

        // Опциональное размещение в круге и в точке.
        s.mRadTOptC { RadMapControlsR.apply }

      )
    }

  }


  val component = ReactComponentB[Props]("NodeMarker")
    .initialState_P { p =>
      State(
        pointOptC  = p.connect { radOpts =>
          OptionUtil.maybe( radOpts.opts.onAdvMap && !radOpts.opts.onGeoLoc ) {
            radOpts.rad.circle.center
          }
        },
        mRadTOptC = p.connect { radOpts =>
          OptionUtil.maybe( radOpts.opts.onGeoLoc ) {
            radOpts.rad
          }
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(nodeMarkerProxy: Props) = component(nodeMarkerProxy)

}
