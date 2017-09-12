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

  // TODO Можно ли как-то унифицировать операции на различными number-типами? Чтобы и Int и Double сразу отрабатывались.

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

  case object IntSum extends IBinaryMathOp[Int] {
    override def apply(arg1: Int, arg2: Int): Int = {
      arg1 + arg2
    }
  }

  case object IntSub extends IBinaryMathOp[Int] {
    override def apply(arg1: Int, arg2: Int): Int = {
      arg1 - arg2
    }
  }



  /** Умножение Double. */
  case object DoubleMult extends IBinaryMathOp[Double] {
    override def apply(arg1: Double, arg2: Double): Double = {
      arg1 * arg2
    }
  }

  /** Деление Double. */
  case object DoubleDiv extends IBinaryMathOp[Double] {
    override def apply(arg1: Double, arg2: Double): Double = {
      arg1 / arg2
    }
  }

  /** Суммирование Double. */
  case object DoubleSum extends IBinaryMathOp[Double] {
    override def apply(arg1: Double, arg2: Double): Double = {
      arg1 + arg2
    }
  }

  /** Вычитание Double. */
  case object DoubleSub extends IBinaryMathOp[Double] {
    override def apply(arg1: Double, arg2: Double): Double = {
      arg1 - arg2
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

  /** Разделить текущий инстанс на указанное число. */
  def /(by: A): T

  /** Умножить текущий инстанс на указанное число. */
  def *(by: A): T

  /** Увеличить текущий инстанс на указанное число. */
  def +(by: A): T

  /** Вычесть из текущего инстанса указанное число. */
  def -(by: A): T

}


/** Готовый к использованию подмешиваемый трейт для операций простой математической модификации
  * какого-то инстанса модели с использованием целых чисел.
  *
  * @tparam T Класс текущей модели. Что-то типа this.type.
  */
trait IntMathModifiers[T] extends IMathModifiers[Int, T] {

  override def /(by: Int): T = applyMathOp(MathOps.IntDiv, by)
  override def *(by: Int): T = applyMathOp(MathOps.IntMult, by)
  override def +(by: Int): T = applyMathOp(MathOps.IntSum, by)
  override def -(by: Int): T = applyMathOp(MathOps.IntSub, by)

}


/** Подмешиваемый трейт для операций простой арифметики над произвольными моделями. */
trait DoubleMathModifiers[T] extends IMathModifiers[Double, T] {

  override def /(by: Double): T = applyMathOp(MathOps.DoubleDiv, by)
  override def *(by: Double): T = applyMathOp(MathOps.DoubleMult, by)
  override def +(by: Double): T = applyMathOp(MathOps.DoubleSum, by)
  override def -(by: Double): T = applyMathOp(MathOps.DoubleSub, by)

}
