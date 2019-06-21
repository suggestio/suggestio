package io.suggest.id.token

import java.time.Instant

import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.06.19 16:21
  * Description: ВременнАя привязка токена: время создания, время жизни и прочее.
  */
object MIdTokenDates {

  implicit def mIdTokenDatesJson: OFormat[MIdTokenDates] = (
    (__ \ "t").format[Int] and
    (__ \ "c").format[Instant] and
    (__ \ "m").formatNullable[Instant]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdTokenDates] = UnivEq.derive

  val modified = GenLens[MIdTokenDates]( _.modified )

}


/** Контейнер данных времени токена.
  *
  * @param ttlSeconds Время жизни токена в секундах с момента создания.
  * @param created Дата-время создания токена.
  * @param modified Дата последнего изменения токена.
  */
case class MIdTokenDates(
                          ttlSeconds        : Int,
                          created           : Instant           = Instant.now(),
                          modified          : Option[Instant]   = None,
                        ) {

  /** До каких пор валиден данный токен. */
  def bestBefore: Instant =
    created plusSeconds ttlSeconds

}
