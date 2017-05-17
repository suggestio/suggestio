package io.suggest.maps.m

import diode.FastEq
import io.suggest.geo.{MGeoCircle, MGeoPoint}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.17 13:19
  * Description: Базовая минимальная модель состояния rad-компонентов.
  * Используется для базового react-компонента [[io.suggest.maps.r.rad.RadMapControlsR]].
  */


/** Базовая логика для FastEq[[MRadT]].
  * Реализации должны дополнять eqv() своими полями. */
trait IMRadTFastEq[T <: MRadT[_]] extends FastEq[T] {
  override def eqv(a: T, b: T): Boolean = {
    (a.circle eq b.circle) &&
      (a.state eq b.state)
  }
}

object MRadT {

  /** Поддержка FastEq для [[MRadT]]. Пригодна для react-компонентов. */
  implicit object MRadTFastEq extends IMRadTFastEq[MRadT[_]]

}


/** Общий интерфейс и утиль для MRad-моделей. */
trait MRadT[T <: MRadT[T]] { this: T =>

  /** Состояние гео-круга в целом, пригодное для сериализации на сервер. */
  val circle      : MGeoCircle

  /** Состояние Rad-компонентов.  */
  val state       : MRadS


  /** Обновление сериализуемого состояния круга. */
  def withCircle(circle2: MGeoCircle): T

  /** Обновление состояния Rad. */
  def withState(state2: MRadS): T

  def withCircleState(circle: MGeoCircle, state: MRadS): T = {
    withCircle(circle)
      .withState(state)
  }


  /** Вычислить текущий центр. */
  def currentCenter: MGeoPoint = {
    state.centerDragging
      .getOrElse( circle.center )
  }

}
