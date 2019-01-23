package io.suggest.sc.m.dia.first

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
      (a.question ===* b.question) &&
      (a.frame ===* b.frame)
    }
  }

  implicit def univEq: UnivEq[MWzFirstS] = UnivEq.derive

}


/** Контейнер данных состояния открытого диалога.
  * Дефолтовые значения
  *
  * @param visible Видим ли отрендеренный диалог на экране?
  *                Нужно для поддержки анимации.
  * @param question Текущая тема вопроса.
  * @param frame тип фрейма: вопрос или сообщение об отказе.
  */
case class MWzFirstS(
                     visible     : Boolean      = false,
                     question    : MWzQuestion  = MWzQuestions.GeoLocPerm,
                     frame       : MWzFrame     = MWzFrames.AskPerm,
                     pending     : Boolean      = false,
                   ) {

  def withVisible(visible: Boolean) = copy(visible = visible)
  def withQuestion(question: MWzQuestion) = copy(question = question)
  def withFrame(frame: MWzFrame) = copy(frame = frame)
  def withPending(pending: Boolean) = copy(pending = pending)

}
