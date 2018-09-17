package io.suggest.ad.edit.m.save

import diode.FastEq
import diode.data.Pot
import io.suggest.ad.edit.m.MAdEditFormInit
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 16:00
  * Description: Модель данных по процессу сохранению карточки на сервер.
  * Процесс изначально простой, но сюда можно будет добавить данные для автосохранения и всё такое.
  *
  * Неявно-пустая модель.
  */
object MSaveS {

  def empty = apply()

  /** Поддержка FastEq для инстансов [[MSaveS]]. */
  implicit object MSaveSFastEq extends FastEq[MSaveS] {
    override def eqv(a: MSaveS, b: MSaveS): Boolean = {
      a.saveReq ===* b.saveReq
    }
  }

  @inline implicit def univEq: UnivEq[MSaveS] = UnivEq.derive

}


/** Модель данных состояния подсистемы сохранения карточки на сервера.
  *
  * @param saveReq Текущий реквест сохранения на сервер, если запущен.
  */
case class MSaveS(
                   saveReq    : Pot[MAdEditFormInit]    = Pot.empty
                 ) {

  def withSaveReq(saveReq: Pot[MAdEditFormInit]) = copy(saveReq = saveReq)

}
