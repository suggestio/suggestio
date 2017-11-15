package io.suggest.sc.search.m

import diode.FastEq
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:32
  * Description: Состояние текстового поиска.
  */
object MScSearchText {

  implicit object MScSearchTextFastEq extends FastEq[MScSearchText] {
    override def eqv(a: MScSearchText, b: MScSearchText): Boolean = {
      a.query ===* b.query
    }
  }

  implicit def univEq: UnivEq[MScSearchText] = UnivEq.derive

}


/** Класс состояния текстового поиска.
  *
  * @param query Текстовый запрос, набираемый юзером.
  */
case class MScSearchText(
                          query   : String
                        )
