package io.suggest.mbill2.m.item.typ

import io.suggest.common.slick.driver.IDriver

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 14:08
 * Description: Поддержка поля item type в slick-моделях.
 */
trait MItemTypeSlick extends IDriver {

  import driver.api._

  def ITYPE_FN = "type"

  /** Поддержка сырого поля типа item'а. */
  trait ItemTypeStrColumn { that: Table[_] =>
    def iTypeStr = column[String](ITYPE_FN, O.SqlType("\"char\""))
  }

  /** Поддержка распарсенного поля item status. */
  trait ItemTypeColumn extends ItemTypeStrColumn { that: Table[_] =>
    def iType = iTypeStr <> (MItemTypes.withNameT, MItemTypes.unapply)
  }

}
