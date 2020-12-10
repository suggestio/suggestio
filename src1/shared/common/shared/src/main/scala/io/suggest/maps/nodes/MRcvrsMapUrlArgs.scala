package io.suggest.maps.nodes

import diode.UseValueEq
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 11:55
  * Description: Модель данных с сервера для сборки ссылки на клиенте.
  */
object MRcvrsMapUrlArgs {

  /** Поддержка play-json. */
  implicit def mRcvrsMapUrlArgsFormat: OFormat[MRcvrsMapUrlArgs] = {
    (__ \ "h").format[Int]
      .inmap[MRcvrsMapUrlArgs](apply, _.hashSum)
  }

  @inline implicit def univEq: UnivEq[MRcvrsMapUrlArgs] = UnivEq.derive

}


/** Контейнер данных для сборки ссылки.
  *
  * @param hashSum Контрольная сумма.
  */
case class MRcvrsMapUrlArgs(
                             hashSum    : Int
                           )
  extends UseValueEq
