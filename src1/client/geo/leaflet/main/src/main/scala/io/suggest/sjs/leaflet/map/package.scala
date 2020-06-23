package io.suggest.sjs.leaflet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 11:24
  */
package object map {

  /** Тип для значения зума.
    * По логике тут целое. Но иногда внезапно, L.getZoom() может возвращать Double.
    */
  type Zoom_t = Int

}
