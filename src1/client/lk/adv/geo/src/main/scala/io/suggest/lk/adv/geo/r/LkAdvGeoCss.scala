package io.suggest.lk.adv.geo.r

import io.suggest.css.ScalaCssDefaults._

class LkAdvGeoCss extends StyleSheet.Standalone {

  import dsl._

  /** To fit popups into geo-map, if too much content, make max-height limited. */
  ".leaflet-popup-content-wrapper" - (
    maxHeight( 300.px ),
    overflowY.auto,
  )

}
