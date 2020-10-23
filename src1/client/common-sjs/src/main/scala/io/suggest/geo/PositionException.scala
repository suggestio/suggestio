package io.suggest.geo

import io.suggest.common.html.HtmlConstants._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.17 18:20
  * Description: Модель исключения геопозиционирования.
  */
final case class PositionException(
                                    code      : Int,
                                    message   : String,
                                    isPermissionDenied: Boolean,
                                    raw       : Any,
                                  )
  extends RuntimeException
{

  override def getMessage: String =
    message + SPACE + `(` + code + `)`

  override def toString = getMessage

}
