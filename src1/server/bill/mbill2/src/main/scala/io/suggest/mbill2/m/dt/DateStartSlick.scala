package io.suggest.mbill2.m.dt

import java.time.OffsetDateTime

import io.suggest.slick.profile.pg.IPgProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:00
 * Description: slick-поддержка для обязательного поля date_start.
 */
trait DateStartSlick extends IPgProfile {

  import profile.api._

  def DATE_START_FN = "date_start"

  /** Добавить колонку dateStart. */
  trait DateStartOpt { that: Table[_] =>
    def dateStartOpt = column[Option[OffsetDateTime]](DATE_START_FN)
  }

}

trait IDateStartOpt {
  def dateStartOpt: Option[OffsetDateTime]
}
