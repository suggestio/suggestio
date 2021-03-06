package io.suggest.spa

import diode.FastEq
import japgolly.univeq._

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
      val aEmpty = a.isEmpty
      val bEmpty = b.isEmpty
      (aEmpty && bEmpty) || {
        !aEmpty && !bEmpty && _eqv(a.get, b.get)
      }
    }

  }


  /** Просто сравнивание значений значений по указателям. */
  implicit object Plain extends OptFastEqHelper[AnyRef] {
    override def _eqv(a: AnyRef, b: AnyRef): Boolean = {
      a eq b
    }
  }

  /** Сравнивание нереференсных типов внутри Option по значениям. */
  object OptValueEq extends OptFastEqHelper[Any] {
    override def _eqv(a: Any, b: Any): Boolean = {
      a == b
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


  /** Сравнивать референсно, либо только результат x.isEmpty() . */
  def IsEmptyEq[T]: FastEq[Option[T]] = new FastEq[Option[T]] {
    override def eqv(a: Option[T], b: Option[T]): Boolean = {
      (a eq b) ||
      (a.isEmpty ==* b.isEmpty)
    }
  }


  /** Для частичного сравнения можно использовать маппинг исходных элементов + FastEq для производного типа. */
  def MapWrapped[T, M](mapF: T => M)(implicit feq: FastEq[M]): FastEq[Option[T]] = {
    new OptFastEqHelper[T] {
      override def _eqv(a: T, b: T): Boolean =
        feq.eqv( mapF(a), mapF(b) )
    }
  }

}
