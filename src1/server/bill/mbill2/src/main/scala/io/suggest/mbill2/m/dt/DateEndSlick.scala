package io.suggest.mbill2.m.dt

import java.time.{Instant, OffsetDateTime}

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

  /** Опциональная колонка date_end. */
  trait DateEndOpt { that: Table[_] =>
    def dateEndOpt = column[Option[OffsetDateTime]](DATE_END_FN)
  }


  /** Обязательная колонка date_end в виде instant без tz. */
  trait DateEndInstantColumn { that: Table[_] =>
    def dateEnd = column[Instant](DATE_END_FN)
  }

}
