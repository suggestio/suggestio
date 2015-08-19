package io.suggest.common

import io.suggest.model.{LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 15:41
 * Description: Константы для таких понятий как "лево" и "право".
 */
trait MHandsBaseT extends ILightEnumeration with HandsT {

  /** Интерфейс будущих экземпляров модели. */
  protected trait ValT extends super.ValT {
    /** Короткий однобуквенный id этого элемента. */
    def strId: String
    /** Если Left, то вернуть Right и наоборот. */
    def inverted: T
    /** Длинное название в нижнем регистре. */
    def name: String
    /** Быстрый и простой доступ, вместо проверки x eq Left. */
    def isLeft: Boolean
    /** Быстрый и простой доступ, вместо проверки x eq Right. */
    def isRight: Boolean

    override def toString = name
  }

  override type T <: ValT

  /** Лево. */
  def Left: T

  /** Реализации Left должны реализовывать этот трейт. */
  protected trait LeftT extends ValT {
    override def name         = LEFT
    override def inverted: T  = Right
    override def isRight      = false
    override def isLeft       = true
  }


  /** Право. */
  def Right: T

  /** Реализации Right должны реализовывать этот трейт. */
  protected trait RightT extends ValT {
    override def name         = RIGHT
    override def inverted: T  = Left
    override def isRight      = true
    override def isLeft       = false
  }

}


/** Код для light-реализаций. */
trait MHandsLightT extends MHandsBaseT with LightEnumeration {

  override def maybeWithName(n: String): Option[T] = {
    if (n == LEFT_ID)
      Some(Left)
    else if (n == RIGHT_ID)
      Some(Right)
    else
      None
  }


  protected trait LeftT extends super.LeftT {
    override def strId = LEFT_ID
  }
  protected trait RightT extends super.RightT {
    override def strId = RIGHT_ID
  }
}


trait HandsT {

  def LEFT_ID = "l"
  def LEFT = "left"

  def RIGHT_ID = "r"
  def RIGHT = "right"

}
