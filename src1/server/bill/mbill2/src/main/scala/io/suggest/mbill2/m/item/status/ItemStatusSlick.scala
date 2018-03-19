package io.suggest.mbill2.m.item.status

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 14:02
 * Description: Аддон для поддержки поля item status в slick-моделях.
 */
trait ItemStatusSlick extends IProfile {

  import profile.api._

  def STATUS_FN = "status"

  /** Поддержка сырого поля статуса. */
  trait ItemStatusStrColumn { that: Table[_] =>
    def statusStr = column[String](STATUS_FN, O.SqlType("\"char\""))
  }

  /** Поддержка отмаппленного поля Item status. */
  trait ItemStatusColumn extends ItemStatusStrColumn { that: Table[_] =>
    def status = statusStr <> (
      MItemStatuses.withValue, MItemStatus.unapplyStrId
    )
  }

}
