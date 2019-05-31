package io.suggest.mbill2.m.dt

import java.time.{Instant, OffsetDateTime}

import io.suggest.slick.profile.pg.IPgProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:29
 * Description: Поле date_created присутствует в разных таблицах, тут трейты для упрощенной поддержка этого дела.
 */

trait DateCreatedSlick extends IPgProfile {

  import profile.api._

  def DATE_CREATED_FN = "date_created"

  /** Добавить колонку date_created. */
  trait DateCreated { that: Table[_] =>
    def dateCreated = column[OffsetDateTime](DATE_CREATED_FN)
  }

  /** Обязательная колонка date_created в instant-виде, без tz. */
  trait DateCreatedInstantColumn { that: Table[_] =>
    def dateCreated = column[Instant](DATE_CREATED_FN)
  }

}
