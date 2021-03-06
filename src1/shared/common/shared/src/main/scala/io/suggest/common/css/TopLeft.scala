package io.suggest.common.css

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 11:01
 * Description: Модели для передачи в шаблоны css-свойств top и left.
 */
trait ITop {
  def top: String
}

trait ITopPx extends ITop with CssSzImplicits {
  def topPx: Int
  override def top = topPx.px
}


trait ILeft {
  def left: String
}
trait ILeftPx extends ILeft with CssSzImplicits {
  def leftPx: Int
  override def left = leftPx.px
}
trait ILeftPc extends ILeft with CssSzImplicits {
  def leftPc: Int
  override def left = leftPc.percents
}


trait ITopLeft extends ITop with ILeft {
  /** Инлайновый рендер стилей. */
  def inlineStyle: Boolean
  /** Используется wide-отображение для элементов карточки?
    * Это поле по смыслу немного отличается от brArgs.isWide, т.к. wide может быть
    * false даже если фон рендерится как wide. */
  def wide: Boolean
}


/** Реализация координат focused-блока. */
object FocusedTopLeft extends ITopLeft with ITopPx with ILeftPc {
  override def topPx  = 50
  override def leftPc = 50
  override def inlineStyle = false
  override def wide = true
}

/** Координаты wide-отображения блока для onlyOneAd-режима. */
case class OnlyOneAdTopLeft(leftPx: Int) extends ITopLeft with ILeftPx with ITopPx {
  override def topPx  = 0
  override def inlineStyle = true
  override def wide = false
}
