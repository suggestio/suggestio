package io.suggest.ueq

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 10:11
  * Description: Кросс-платформенная утиль для UnivEq.
  */
object UnivEqUtil {

  /** Расширенные операции для UnivEq. */
  implicit final class UnivEqExtOps2[A <: AnyRef](private val a: A) extends AnyVal {

    /**
      * Поддержка eq-сравнивания для UnivEq.
      * Вместо "eq*" пишем "===*", потому что смесь букв со знаками в названии метода не работает в idea.
      */
    @inline def ===*[B >: A <: AnyRef : UnivEq](b: B): Boolean = {
      a eq b
    }

  }


  // Поддержка разных типов для UnivEq.
  @inline implicit def doubleUe           : UnivEq[Double]          = UnivEq.force
  @inline implicit def floadUe            : UnivEq[Float]           = UnivEq.force

  @inline implicit def seqUe[T: UnivEq]   : UnivEq[Seq[T]]          = UnivEq.force

  @inline implicit def throwableUe        : UnivEq[Throwable]       = UnivEq.force

  @inline implicit def fun1Ue[T, R]       : UnivEq[(T) => R]        = UnivEq.force

}
