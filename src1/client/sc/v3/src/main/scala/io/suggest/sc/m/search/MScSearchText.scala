package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:32
  * Description: Состояние текстового поиска.
  */
object MScSearchText {

  def empty = apply()

  /** Поддержка FastEq для инстансов [[MScSearchText]]. */
  implicit object MScSearchTextFastEq extends FastEq[MScSearchText] {
    override def eqv(a: MScSearchText, b: MScSearchText): Boolean = {
      (a.query          ===* b.query) &&
        (a.searchQuery    ===* b.searchQuery) &&
        (a.searchTimerId  ===* b.searchTimerId)
    }
  }

  implicit def univEq: UnivEq[MScSearchText] = UnivEq.derive

}


/** Класс состояния текстового поиска.
  *
  * @param query Состояние текста в текстовом поле на экране.
  * @param searchQuery Последний фактический поисковый запрос.
  *                    Сюда вписывается готовое для поиска значение.
  */
case class MScSearchText(
                          query             : String                = "",
                          searchQuery       : Pot[String]           = Pot.empty,
                          searchTimerId     : Option[Int]           = None
                        ) {

  def withQuery(query: String)            = copy(query = query)
  def withSearchQueryTimer(searchQuery: Pot[String], searchTimerId: Option[Int]) = {
    copy(searchQuery = searchQuery, searchTimerId = searchTimerId)
  }

}
