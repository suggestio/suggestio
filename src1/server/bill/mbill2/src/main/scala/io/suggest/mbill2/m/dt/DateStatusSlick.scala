package io.suggest.mbill2.m.dt

import io.suggest.common.slick.driver.IPgDriver
import org.joda.time.DateTime

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
    def dateStatus    = column[DateTime](DATE_STATUS_FN)
  }

}


trait IDateStatus {
  def dateStatus: DateTime
}
