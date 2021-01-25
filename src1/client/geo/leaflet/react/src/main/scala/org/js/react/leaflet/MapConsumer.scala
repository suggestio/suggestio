package org.js.react.leaflet

import io.suggest.sjs.leaflet.map.LMap
import japgolly.scalajs.react.raw
import japgolly.scalajs.react.raw.React

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:38
  */
trait MapConsumerProps extends js.Object {
  val children: js.Function1[LMap, raw.React.Element | Null]
}


@js.native
@JSImport(REACT_LEAFLET_PACKAGE, "MapConsumer")
object MapConsumer extends js.Function1[MapContainerProps, raw.React.Element | Null] {
  override def apply(props: MapContainerProps): React.Element | Null = js.native
}
