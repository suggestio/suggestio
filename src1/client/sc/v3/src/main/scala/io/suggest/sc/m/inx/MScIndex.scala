package io.suggest.sc.m.inx

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.m.search.MScSearch
import io.suggest.sc.sc3.MSc3IndexResp
import io.suggest.sc.styl.ScCss
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:31
  * Description: Модель, описывающая индекс выдачи и его состояния.
  */
object MScIndex {

  implicit object MScIndexFastEq extends FastEq[MScIndex] {
    override def eqv(a: MScIndex, b: MScIndex): Boolean = {
      (a.state      ===* b.state) &&
        (a.resp     ===* b.resp) &&
        (a.welcome  ===* b.welcome) &&
        (a.search   ===* b.search) &&
        (a.scCss    ===* b.scCss)
    }
  }

  implicit def univEq: UnivEq[MScIndex] = UnivEq.derive

}

case class MScIndex(
                     state      : MScIndexState           = MScIndexState.empty,
                     resp       : Pot[MSc3IndexResp]      = Pot.empty,
                     welcome    : Option[MWelcomeState]   = None,
                     search     : MScSearch,
                     scCss      : ScCss,
                   ) {

  def withState(state: MScIndexState)             = copy(state = state)
  def withResp(resp: Pot[MSc3IndexResp])          = copy(resp = resp)
  def withWelcome(welcome: Option[MWelcomeState]) = copy(welcome = welcome)
  def withSearch(search: MScSearch)               = copy(search = search)
  def withScCss(scCss: ScCss)                     = copy(scCss = scCss)

}
