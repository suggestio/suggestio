package io.suggest.bill

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.01.17 19:12
  * Description: JVM-поддержка для кросс-платформенной модели MCurrencies.
  */
object MCurrenciesJvm {

  /** Поддержка форматирования валюты. */
  implicit val CURRENCY_FORMAT: Format[MCurrency] = {
    __.format[String]
      .inmap[MCurrency](
        MCurrencies.withName,
        _.currencyCode
      )
  }

}
