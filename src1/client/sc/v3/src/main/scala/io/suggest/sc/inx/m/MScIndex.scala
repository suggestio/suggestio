package io.suggest.sc.inx.m

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.search.m.MScSearch

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:31
  * Description: Модель, описывающая индекс выдачи и его состояния.
  */
object MScIndex {

  implicit object MScIndexFastEq extends FastEq[MScIndex] {
    override def eqv(a: MScIndex, b: MScIndex): Boolean = {
      (a.state eq b.state) &&
        (a.resp eq b.resp) &&
        (a.welcome eq b.welcome) &&
        (a.search eq b.search)
    }
  }

}

case class MScIndex(
                     state      : MScIndexState,
                     resp       : Pot[MSc3IndexResp]      = Pot.empty,
                     welcome    : Option[MWelcomeState]   = None,
                     search     : MScSearch
                   ) {

  def withState(state: MScIndexState)             = copy(state = state)
  def withResp(resp: Pot[MSc3IndexResp])          = copy(resp = resp)
  def withWelcome(welcome: Option[MWelcomeState]) = copy(welcome = welcome)
  def withSearch(search: MScSearch)               = copy(search = search)

}
