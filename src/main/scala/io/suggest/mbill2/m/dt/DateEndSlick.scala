package io.suggest.mbill2.m.dt

import io.suggest.common.slick.driver.IPgDriver
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:01
 * Description: Поддержка обязательного поля date_end.
 */
trait DateEndSlick extends IPgDriver {

  import driver.api._

  def DATE_END_FN = "date_end"

  /** Добавить колонку dateEnd. */
  trait DateEndColumn { that: Table[_] =>
    def dateEnd = column[DateTime](DATE_END_FN)
  }

}


trait IDateEnd {
  def dateEnd: DateTime
}
