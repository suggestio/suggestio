package io.suggest.lk.adv.direct.m

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.16 14:12
 * Description: Контейнер переменных для id'шников, содержащих cityId и опциональный ngId.
 */
case class CityNgIdOpt(
  cityId  : String,
  ngIdOpt : Option[String]
)
