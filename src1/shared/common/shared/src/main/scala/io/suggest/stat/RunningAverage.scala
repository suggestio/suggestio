package io.suggest.stat

import japgolly.univeq._

import scala.collection.immutable.Queue
import scala.math.Numeric

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 15:29
  * Description: Модель сбора короткой статистики по значению для нужд рассчёта скользящего среднего.
  */
final case class RunningAverage[V](
                                    maxLen     : Int,
                                    queue      : Queue[V]      = Queue.empty,
                                    knownLen   : Option[Int]   = None,
                                  ) {

  private[this] lazy val _qLen = queue.length
  def length = knownLen getOrElse _qLen

}

object RunningAverage {

  @inline implicit def univEq[V: UnivEq]: UnivEq[RunningAverage[V]] = UnivEq.derive

  implicit final class RunningAvgOpsExt[V]( private val ra2: RunningAverage[V] ) extends AnyVal {

    /** Добавление значения в набор. */
    def push(v: V): RunningAverage[V] = {
      val ra2Len = ra2.length
      val (q1, len2) = if (ra2Len >= ra2.maxLen)
        ra2.queue.tail -> ra2Len
      else
        ra2.queue -> (ra2Len + 1)

      ra2.copy(
        queue     = q1.enqueue(v),
        // Сохраняем текущий инстанс knownLen, когда возможно:
        knownLen  = if (ra2.knownLen contains[Int] len2) ra2.knownLen else Some(len2),
      )
    }

    /** Суммирование, если есть хотя бы один элемент в наборе. */
    def sumOpt(implicit num: Numeric[V]): Option[V] = {
      ra2
        .queue
        .reduceOption( num.plus )
    }

    /** Суммирование. */
    def sum(implicit num: Numeric[V]): V =
      sumOpt getOrElse num.zero

    /** Вычислить среднее.
      *
      * @return Double, т.к. среднее обычно нецелое, даже если все слагаемые - целые.
      */
    def average(implicit num: Numeric[V]): Option[Double] = {
      for (sum <- sumOpt) yield
        num.toDouble(sum) / ra2.length
    }


    /** Срезать возможные экстремумы, для сглаживания среднего.
      *
      * @param maxFactor Сколько отступать от значения экстремума.
      * @param maxRemovedPerSide Макс.кол-во спиливаемых экстремумов в каждом направлении.
      *                          Если кол-во экстремумов на одной стороне превышает указанное значение,
      *                          то они не считаются экстремумами, и не удаляются.
      * @return Обновлённый инстанс RunningAverage2.
      */
    def stripExtemes(maxFactor: Double = 0.9, maxRemovedPerSide: Int = ra2.maxLen / 5)
                    (implicit num: Numeric[V]): RunningAverage[V] = {
      if (maxFactor >= 1.0 || maxFactor <= 0) {
        throw new IllegalArgumentException

      } else if (ra2.length <= 4) {
        ra2

      } else {
        /** Функция спиливания экстремумов снизу либо сверху:
          *
          * @param maxValue Нижний или верхний экстремум .
          * @param isValueOkForLimit Функций-компаратор, сравнивает значение 1 и предел 2, где true означает,
          *                          что значение 1 укладывается в предельное значение 2.
          * @return Обновлённый инстанс [[RunningAverage]], где отсуствуют острые экстремумы.
          */
        def __doIt(ra: RunningAverage[V], maxValue: V, isValueOkForLimit: (Double, Double) => Boolean): RunningAverage[V] = {
          val limitedValueMax = num.toDouble( maxValue ) * maxFactor

          var filteredCount = 0
          val q2 = ra.queue.filter { v =>
            val isKeep = isValueOkForLimit( num.toDouble(v), limitedValueMax )
            if (!isKeep) filteredCount += 1
            isKeep
          }

          if (filteredCount <= maxRemovedPerSide) {
            ra.copy(
              queue     = q2,
              knownLen  = ra.knownLen.map(_ - filteredCount),
            )
          } else {
            ra
          }
        }

        val maxStripped = __doIt(ra2, ra2.queue.max, _ <= _)
        val minMaxStripped = __doIt(maxStripped, maxStripped.queue.min, _ >= _)

        minMaxStripped
      }
    }

  }

}
