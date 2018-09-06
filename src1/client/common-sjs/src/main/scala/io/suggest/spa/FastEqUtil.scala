package io.suggest.spa

import diode.FastEq
import japgolly.univeq._

import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.17 10:30
  * Description: Всякая разная утиль для FastEq.
  */
object FastEqUtil {

  /** Условно быстрое последовательное сравнивание двух Map'ов по ключам и значениям. */
  def KvCollFastEq[K, V, M[_] <: Iterable[_]](implicit feqK: FastEq[K], feqV: FastEq[V]): FastEq[M[(K, V)]] = {
    val collFastEq = new FastEq[(K, V)] {
      override def eqv(a: (K, V), b: (K, V)): Boolean = {
        feqK.eqv(a._1, b._1) &&
          feqV.eqv(a._2, b._2)
      }
    }
    CollFastEq[(K, V), M]( collFastEq )
  }


  /** Условно быстрое ~O((N+1)*M) сравнивание двух коллекций с запуском O(M) FastEq[T] над каждой парой элементов.
    *
    * @param feq FastEq для любых элементов коллекции.
    * @tparam T Тип одного элемента коллекции
    * @tparam M Тип коллекции.
    *           Используется Seq[T] вместо Seq[_], потому что компилятор плохо понимает типы внутри forall().
    * @return
    */
  def CollFastEq[T, M[_] <: Iterable[_]](implicit feq: FastEq[T]): FastEq[M[T]] = {
    new FastEq[M[T]] {
      override def eqv(a: M[T], b: M[T]): Boolean = {
        // Сравнить длины коллекций, т.к. поштучное сравнивание может не учитывать разность длин.
        (a eq b) || {
          (a.size ==* b.size) && {
            a.iterator
              .zip(b.iterator)
              .forall { case (x: T, y: T) =>
                feq.eqv(x, y)
              }
          }
        }
      }
    }
  }


  implicit def AnyValueEq[T] = {
    new FastEq[T] {
      override def eqv(a: T, b: T) = a == b
    }
  }

  /** Искуственная подстанова FastEq произвольного типа с eq-сравниванием. */
  def AnyRefFastEq[T <: AnyRef]: FastEq[T] = {
    FastEq.AnyRefEq.asInstanceOf[FastEq[T]]
  }

}
