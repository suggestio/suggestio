package io.suggest.proto.http.cookie

import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.2020 15:15
  * Description: Представление рантаймового состояния кукиса для хранение внутри состояния circuit.
  * Сюда складируется как распарсенный кукис, так и какие-либо метаданные (время получения на клиенте).
  */
object MCookieState {

  @inline implicit def univEq: UnivEq[MCookieState] = UnivEq.derive

}


/** Контейнер состояния кукиса в рантайме.
  *
  * @param parsed Распарсенный кукис.
  * @param meta Метаданные кукиса.
  */
final case class MCookieState(
                               parsed         : MHttpCookieParsed,
                               meta           : MCookieMeta,
                             ) {

  def toRawCookie = MHttpCookie(
    setCookieHeaderValue = parsed.toSetCookie,
    meta = meta,
  )

}
