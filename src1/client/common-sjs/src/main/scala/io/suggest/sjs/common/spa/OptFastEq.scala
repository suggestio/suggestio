package io.suggest.sjs.common.spa

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 12:36
  * Description: Поддержка FastEq для опциональных значений, генерируемых динамически.
  */
object OptFastEq {

  sealed trait OptFastEqHelper[T] extends FastEq[Option[T]] {

    def _eqv(a: T, b: T): Boolean

    override def eqv(a: Option[T], b: Option[T]): Boolean = {
      (a eq b) || ((a,b) match {
        case (Some(x), Some(y))   => _eqv(x, y)
        case (None, None)         => true
        case _                    => false
      })
    }

  }


  /** Просто сравнивание значений значений по указателям. */
  implicit object Plain extends OptFastEqHelper[AnyRef] {
    override def _eqv(a: AnyRef, b: AnyRef): Boolean = {
      a eq b
    }
  }


  /** Сравнивание значений с помощью FastEq для типа значения внутри Option'а. */
  implicit def Wrapped[T](implicit feq: FastEq[T]): FastEq[Option[T]] = {
    new OptFastEqHelper[T] {
      override def _eqv(a: T, b: T): Boolean = {
        feq.eqv(a, b)
      }
    }
  }

}


