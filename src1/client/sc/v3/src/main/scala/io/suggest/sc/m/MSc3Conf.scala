package io.suggest.sc.m

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 22:18
  * Description: Модель конфига выдачи. Константы как всегда задаются сервером.
  */
object MSc3Conf {

  implicit object MSc3ConfFastEq extends FastEq[MSc3Conf] {
    override def eqv(a: MSc3Conf, b: MSc3Conf): Boolean = {
      a.rcvrsMapUrl ===* b.rcvrsMapUrl
    }
  }

  implicit def univEq: UnivEq[MSc3Conf] = UnivEq.derive

}

case class MSc3Conf(
                     rcvrsMapUrl: String
                   )
