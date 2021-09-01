package io.suggest.sc.m.dia.first

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:43
  * Description: Явно пустая модель, живёт внутри Option'а.
  */
object MWzFirstS {

  implicit object MWzFirstSEq extends FastEq[MWzFirstS] {
    override def eqv(a: MWzFirstS, b: MWzFirstS): Boolean = {
      (a.phase ===* b.phase) &&
      (a.frame ===* b.frame)
    }
  }

  @inline implicit def univEq: UnivEq[MWzFirstS] = UnivEq.force

  def phase     = GenLens[MWzFirstS](_.phase)
  def frame     = GenLens[MWzFirstS](_.frame)

}


/** Контейнер данных состояния открытого диалога.
  * Дефолтовые значения
  *
  * @param phase Текущая тема вопроса.
  * @param frame тип фрейма: вопрос или сообщение об отказе.
  */
case class MWzFirstS(
                      phase       : MWzPhase,
                      frame       : MWzFrame,
                   )
