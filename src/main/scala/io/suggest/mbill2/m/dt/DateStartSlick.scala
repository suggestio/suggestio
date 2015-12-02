package io.suggest.mbill2.m.dt

import io.suggest.common.slick.driver.IPgDriver
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:00
 * Description: slick-поддержка для обязательного поля date_start.
 */
trait DateStartSlick extends IPgDriver {

  import driver.api._

  def DATE_START_FN = "date_start"

  /** Добавить колонку dateStart. */
  trait DateStartColumn { that: Table[_] =>
    def dateStart = column[DateTime](DATE_START_FN)
  }

}

trait IDateStart {
  def dateStart: DateTime
}
