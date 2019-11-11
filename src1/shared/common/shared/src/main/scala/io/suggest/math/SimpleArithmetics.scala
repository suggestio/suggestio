package io.suggest.math

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 12:02
  * Description: Абстрактные примитивные арифметические операции над произвольными типами данных.
  */

object SimpleArithmetics {

  /** Дубляж Numeric.IntIsIntegral, но без mkNumericOps(), который конфликтует с Fractional.mkNumericOps().
    * Нужно для поддержки операций деления с потерями для int-значений.
    */
  trait IntIsConflicted extends Numeric[Int] {
    @inline private def n = implicitly[Numeric[Int]]
    def plus(x: Int, y: Int): Int = n.plus(x, y)
    def minus(x: Int, y: Int): Int = n.minus(x, y)
    def times(x: Int, y: Int): Int = n.times(x, y)
    def negate(x: Int): Int = n.negate(x)
    def fromInt(x: Int): Int = n.fromInt(x)
    def toInt(x: Int): Int = n.toInt(x)
    def toLong(x: Int): Long = n.toLong(x)
    def toFloat(x: Int): Float = n.toFloat(x)
    def toDouble(x: Int): Double = n.toDouble(x)
  }

  implicit object FractionalIntLossy
    extends IntIsConflicted
    with Ordering.IntOrdering
    with Fractional[Int]
  {
    override def div(x: Int, y: Int): Int = x / y
    override def parseString(str: String): Option[Int] = Try(str.toInt).toOption
  }

  implicit class SimpleArithmeticsOps[T, Value_t](val v0: T )(implicit support: SimpleArithmetics[T, Value_t]) {

    /** Разделить текущий инстанс на указанное число. */
    def /(by: Value_t)(implicit frac: Fractional[Value_t]): T =
      support.applyMathOp( v0 )( frac.div(_, by) )

    /** Умножить текущий инстанс на указанное число. */
    def *(by: Value_t)(implicit numeric: Numeric[Value_t]): T =
      support.applyMathOp( v0 )( numeric.times(_, by) )

    /** Увеличить текущий инстанс на указанное число. */
    def +(by: Value_t)(implicit numeric: Numeric[Value_t]): T =
      support.applyMathOp( v0 )( numeric.plus(_, by) )

    /** Вычесть из текущего инстанса указанное число. */
    def -(by: Value_t)(implicit numeric: Numeric[Value_t]): T =
      support.applyMathOp( v0 )( numeric.minus(_, by) )

  }

}

/** Интерфейс для typeclass'а применения абстрактной арифметической операции.
  *
  * @tparam T Тип левой части выражения (обычно - класс с данными).
  * @tparam Value_t Тип правой части выражения (обычно - числовой).
  */
trait SimpleArithmetics[T, Value_t] {
  def applyMathOp(v: T)(op: Value_t => Value_t): T
}


