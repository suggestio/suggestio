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

  /** Условно быстрое ~O((N+1)*M) сравнивание двух коллекций с запуском O(M) FastEq[T] над каждой парой элементов.
    *
    * @param feq FastEq для любых элементов коллекции.
    * @tparam T Тип одного элемента коллекции
    * @tparam M Тип коллекции.
    *           Используется Seq[T] вместо Seq[_], потому что компилятор плохо понимает типы внутри forall().
    * @return
    */
  def DeepCollFastEq[T, M[_] <: Seq[T]](implicit feq: FastEq[T]): FastEq[M[T]] = {
    new FastEq[M[T]] {
      override def eqv(a: M[T], b: M[T]): Boolean = {
        // Сравнить длины коллекций, т.к. поштучное сравнивание может не учитывать разность длин.
        a.length ==* b.length && {
          a.iterator
            .zip(b.iterator)
            .forall {
              (feq.eqv _).tupled
            }
        }
      }
    }
  }

}
