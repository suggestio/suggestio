package io.suggest.mbill2.m.item.cols

import io.suggest.common.slick.driver.IPgDriver
import io.suggest.model.sc.common.{SinkShowLevels, SinkShowLevel}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 22:24
 * Description: Поддержка опционального поля show levels (sls).
 */
trait SlsOptSlick extends IPgDriver {

  import driver.api._

  def SLS_FN = "sls"

  trait SlsOptRawColumn { that: Table[_] =>

    def slsOptRaw = column[Option[Seq[String]]](SLS_FN)

  }


  trait SlsColumn extends SlsOptRawColumn { that: Table[_] =>

    private def _applySlsOptRaw(slsOptRaw: Option[Seq[String]]): Set[SinkShowLevel] = {
      slsOptRaw.fold {
        Set.empty[SinkShowLevel]
      } {
        SinkShowLevels.applySlsSet
      }
    }

    private def _unapplySls(sls: Set[SinkShowLevel]): Option[Option[Seq[String]]] = {
      val v = if (sls.isEmpty) {
        None
      } else {
        SinkShowLevels.unapplySlsSet(sls)
      }
      Some(v)
    }

    def sls = slsOptRaw <> (_applySlsOptRaw, _unapplySls)

  }

}


trait ISls {
  def sls: Set[SinkShowLevel]
}
