package io.suggest.mbill2.m.item.cols

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:08
 * Description: slick-поддержка для поля reason, которое содержит причину отказа в размещении.
 */
trait ReasonOptSlick extends IProfile {

  import profile.api._

  def REASON_FN = "reason"

  trait ReasonOptColumn { that: Table[_] =>
    def reasonOpt = column[Option[String]](REASON_FN, O.Length(512))
  }

}
