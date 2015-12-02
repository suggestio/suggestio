package io.suggest.mbill2.m.item.adv.cols

import io.suggest.common.slick.driver.IPgDriver
import io.suggest.model.sc.common.{SinkShowLevel, SinkShowLevels}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 16:22
 * Description: Поддержка поля sls (show levels). Используется в таблице прямого размещения.
 */
trait SlsSlick extends IPgDriver {

  import driver.api._

  def SLS_FN = "sls"

  /** Поддержка сырой колонки sls. */
  trait SlsRawColumn { that: Table[_] =>
    def slsRaw = column[Seq[String]](SLS_FN, O.SqlType("varchar[]"))
  }


  /** Поддержка отмаппленной колонки sls. */
  trait SlsColumn extends SlsRawColumn { that: Table[_] =>
    def sls = slsRaw <> (SinkShowLevels.applySlsSet, SinkShowLevels.unapplySlsSet)
  }

}


trait ISls {
  def sls: Set[SinkShowLevel]
}
