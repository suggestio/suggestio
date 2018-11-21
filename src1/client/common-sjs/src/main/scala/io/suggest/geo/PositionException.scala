package io.suggest.geo

import io.suggest.common.html.HtmlConstants
import org.scalajs.dom.PositionError

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.17 18:20
  * Description: Модель исключения геопозиционирования.
  */
case class PositionException(pe: PositionError) extends RuntimeException {

  override def getMessage: String = {
    pe.code.toString + HtmlConstants.SPACE + pe.message
  }

  override final def toString = getMessage

}
