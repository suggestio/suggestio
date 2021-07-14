package io.suggest.lk.adv.geo.r

import io.suggest.css.ScalaCssDefaults._

class LkAdvGeoCss extends StyleSheet.Standalone {

  import dsl._

  /** To fit popups into geo-map, if too much content, make max-height limited. */
  ".leaflet-popup-content-wrapper" - (
    maxHeight( 270.px ),    // 0.5 * [radmap container height] - 30px (for popup outer borders, tail, etc) = 270px.
    overflowY.auto,
  )

}
