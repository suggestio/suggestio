package io.suggest.mbill2.m.dt

import java.time.OffsetDateTime

import io.suggest.common.slick.driver.IPgDriver

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 14:58
  * Description: Поддержка обязательного поля date_status для slick-моделей.
  */
trait DateStatusSlick extends IPgDriver {

  import driver.api._

  def DATE_STATUS_FN = "date_status"

  /** Трейт, добавляющий поддержку колонки date_status. */
  trait DateStatusColumn { that: Table[_] =>
    def dateStatus    = column[OffsetDateTime](DATE_STATUS_FN)
  }

}


trait IDateStatus {
  def dateStatus: OffsetDateTime
}
