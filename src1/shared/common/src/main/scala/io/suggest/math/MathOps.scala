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


/** Интерфейс для подмешиваемых трейтов, реализующих простые математические операции
  * на произвольных классах-моделях.
  *
  * @tparam A Тип значения объекта математических операций. Например Int.
  * @tparam T Тип класса модели.
  */
trait IMathModifiers[A, T] {

  protected[this] def applyMathOp(op: IBinaryMathOp[A], arg2: A): T

  /** Разделить текущий инстанс на целое число. */
  def /(by: A): T

  /** Умножить текущий инстанс на целое число. */
  def *(by: A): T

}

/** Готовый к использованию подмешиваемый трейт для операций простой математической модификации
  * какого-то инстанса модели с использованием целых чисел.
  *
  * @tparam T Класс текущей модели. Что-то типа this.type.
  */
trait IntMathModifiers[T] extends IMathModifiers[Int, T] {

  /** Разделить текущий инстанс на целое число. */
  override def /(by: Int): T = applyMathOp(MathOps.IntDiv, by)

  /** Умножить текущий инстанс на целое число. */
  override def *(by: Int): T = applyMathOp(MathOps.IntMult, by)

}
