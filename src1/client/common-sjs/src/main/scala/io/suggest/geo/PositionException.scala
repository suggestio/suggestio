package io.suggest.geo

import io.suggest.common.html.HtmlConstants.{SPACE, `(`, `)`}
import org.scalajs.dom.PositionError

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.17 18:20
  * Description: Модель исключения геопозиционирования.
  */
case class PositionException(domError: PositionError) extends RuntimeException {

  override def getMessage: String =
    domError.message + SPACE + `(` + domError.code + `)`

  override final def toString = getMessage

}
