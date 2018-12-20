package io.suggest.common.geom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.12.18 18:01
  */
package object coord {

  /** Тип значения гео-координаты: lat, lon.
    * Double не подходит, т.к. необходимо управлять точностью.
    */
  type GeoCoord_t = BigDecimal

}
