package io.suggest.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 14:44
  */
package object ipgeobase {

  // Type of city ids, used in ipgeobase dump.
  type CityId_t = Short

  def StringToCityId(cityId: String): CityId_t = cityId.toShort

}
