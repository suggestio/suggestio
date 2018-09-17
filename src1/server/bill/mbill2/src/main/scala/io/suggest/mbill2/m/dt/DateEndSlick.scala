package io.suggest.mbill2.m.dt

import java.time.OffsetDateTime

import io.suggest.slick.profile.pg.IPgProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:01
 * Description: Поддержка обязательного поля date_end.
 */
trait DateEndSlick extends IPgProfile {

  import profile.api._

  def DATE_END_FN = "date_end"

  /** Добавить колонку dateEnd. */
  trait DateEndOpt { that: Table[_] =>
    def dateEndOpt = column[Option[OffsetDateTime]](DATE_END_FN)
  }

}
