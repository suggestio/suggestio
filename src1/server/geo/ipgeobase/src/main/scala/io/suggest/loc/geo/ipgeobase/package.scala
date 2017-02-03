package io.suggest.loc.geo

import io.suggest.es.util.SioEsUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 14:44
  */
package object ipgeobase {

  // Конкретный целочисленный тип CityId_t и его поддержка:
  type CityId_t = Short
  def EsCityIdFieldType = SioEsUtil.DocFieldTypes.short
  def StringToCityId(cityId: String): CityId_t = cityId.toShort

}
