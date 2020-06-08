package io.suggest.sc.m.dia.first

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.v.dia.first.WzFirstCss
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
      (a.visible ==* b.visible) &&
      (a.phase ===* b.phase) &&
      (a.frame ===* b.frame) &&
      (a.css ===* b.css)
    }
  }

  @inline implicit def univEq: UnivEq[MWzFirstS] = UnivEq.force

  def visible   = GenLens[MWzFirstS](_.visible)
  def phase     = GenLens[MWzFirstS](_.phase)
  def frame     = GenLens[MWzFirstS](_.frame)
  def css       = GenLens[MWzFirstS](_.css)
  def unSubscribe = GenLens[MWzFirstS](_.unSubscribe)

}


/** Контейнер данных состояния открытого диалога.
  * Дефолтовые значения
  *
  * @param visible Видим ли отрендеренный диалог на экране?
  *                Нужно для поддержки анимации.
  * @param phase Текущая тема вопроса.
  * @param frame тип фрейма: вопрос или сообщение об отказе.
  * @param css доп.вёрстка, которая касается только wz-first.
  */
case class MWzFirstS(
                      visible     : Boolean,
                      phase       : MWzPhase,
                      frame       : MWzFrame,
                      css         : WzFirstCss,
                      unSubscribe : Pot[() => Unit],
                   )
