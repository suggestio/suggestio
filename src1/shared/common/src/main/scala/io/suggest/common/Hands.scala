package io.suggest.common

import enumeratum._
import io.suggest.primo.IStrId

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 15:41
 * Description: Константы для таких понятий как "лево" и "право".
 */
sealed abstract class MHand extends EnumEntry with IStrId {

  /** Если Left, то вернуть Right и наоборот. */
  def inverted: MHand

  /** Длинное название в нижнем регистре. */
  def name: String

  /** Быстрый и простой доступ, вместо проверки x eq Left. */
  def isLeft: Boolean

  /** Быстрый и простой доступ, вместо проверки x eq Right. */
  def isRight: Boolean


  override final def entryName = strId
  override final def toString = name

}


/** Модель понятий "лево" и "право". */
object MHands extends Enum[MHand] {

  /** Лево. */
  case object Left extends MHand {
    override def strId        = "l"
    override def name         = "left"
    override def inverted     = Right
    override def isRight      = false
    override def isLeft       = true
  }

  /** Право. */
  case object Right extends MHand {
    override def strId        = "r"
    override def name         = "right"
    override def inverted     = Left
    override def isRight      = true
    override def isLeft       = false
  }

  override def values = findValues

}
