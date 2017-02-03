package io.suggest.mbill2.m.geo.shape

import io.suggest.geo.GeoShape

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:46
  * Description: Интерфейс к полю модели с опциональным гео-шейпом.
  */
trait IGeoShapeOpt {

  def geoShape        : Option[GeoShape]

}
