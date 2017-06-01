package io.suggest.math

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 12:02
  * Description: Абстрактные примитивные математические операции в виде type-class'ов.
  */
trait IBinaryMathOp[A] {

  /** Применить математическую операцию для двух аргументов одного типа. */
  def apply(arg1: A, arg2: A): A

}

object MathOps {

  /** Целочисленное умножение. */
  case object IntMult extends IBinaryMathOp[Int] {
    override def apply(arg1: Int, arg2: Int): Int = {
      arg1 * arg2
    }
  }

  /** Целочисленное деление без остатка. */
  case object IntDiv extends IBinaryMathOp[Int] {
    override def apply(arg1: Int, arg2: Int): Int = {
      arg1 / arg2
    }
  }

}


/** Подмешиваемый трейт для операций простой математической модификации какого-то инстанса модели.
  *
  * @tparam T Класс текущей модели. Что-то типа this.type.
  */
trait IntMathModifiers[T] {

  protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): T

  /** Разделить текущий инстанс на целое число. */
  def /(by: Int): T = applyMathOp(MathOps.IntDiv, by)

  /** Умножить текущий инстанс на целое число. */
  def *(by: Int): T = applyMathOp(MathOps.IntMult, by)

}
