package io.suggest.sc.search.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:32
  * Description: Состояние текстового поиска.
  */
object MScSearchText {

  implicit object MScSearchTextFastEq extends FastEq[MScSearchText] {
    override def eqv(a: MScSearchText, b: MScSearchText): Boolean = {
      a.query eq b.query
    }
  }

}


/** Класс состояния текстового поиска.
  *
  * @param query Текстовый запрос, набираемый юзером.
  */
case class MScSearchText(
                          query   : String
                        )
