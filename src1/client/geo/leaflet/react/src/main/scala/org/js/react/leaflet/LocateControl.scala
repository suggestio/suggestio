package org.js.react.leaflet

import io.suggest.sjs.leaflet.control.locate.{LocateControlOptions, LocateControl => LeafletLocateControl}
import org.js.react.leaflet.core.createControlComponent
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.01.2021 12:20
  */
object LocateControl {

  val componentRaw = createControlComponent[LeafletLocateControl, LocateControlOptions](
    createInstance = { props =>
      new LeafletLocateControl( props )
    },
  )

  val component = JsForwardRefComponent[LocateControlOptions, Children.None, LeafletLocateControl]( componentRaw )

}
