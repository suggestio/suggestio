package io.suggest.sjs.common.stat

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 15:29
  * Description: mutable-объект, занимающийся сборкой статистики по значению и её примитивной обработкой.
  * Умеет вычислять скользящее среднее.
  */
trait RunningAverage {

  type V

  def length: Int

  private def I0: Int = 0

  private var _i: Int = I0

  private var _countMeasured: Int = I0

  def countMeasured = _countMeasured

  private val _measurments: js.Array[V] = new js.Array[V](length)

  def pushValue(v: V): Unit = {
    _measurments(_i) = v

    // Выставить новый индекс текущей ячейки для измерения.
    val i2 = _i + 1
    _i = if (i2 >= length) {
      I0
    } else {
      i2
    }

    // Обновить счётчик измеренных значений.
    if (_countMeasured < length)
      _countMeasured += 1
  }

  def sum(a: V, b: V): V

  def divide(a: V, divider: Int): V

  def measurments: Seq[V] = {
    (_measurments: Seq[V])
      .view(I0, _countMeasured)
  }

  def average: Option[V] = {
    val wrap = measurments
    wrap
      .reduceOption(sum)
      .fold [Option[V]] {
        wrap.headOption
          .map(divide(_, 1))
      } { sumAll =>
        val avg = divide(sumAll, _countMeasured)
        Some(avg)
      }
  }

  def lastOption: Option[V] = {
    if (_countMeasured > 0)
      Some( _measurments(_i) )
    else
      None
  }

}


/** Фильтрация какой-то доли минимальных и максимальных значений. */
trait TopBottomFiltered extends RunningAverage {

  /**
    * Для фильтруемых сбоку значений.
    * Обычно стоит отсеивать 10% сверху и снизу.
    */
  def filteredPerSide: Double = 0.1

  override def measurments: Seq[V] = {
    val measurments = super.measurments
    val l = countMeasured
    val dropPerSide = Math.round( l * filteredPerSide ).toInt
    if (dropPerSide <= 0) {
      measurments
    } else {
      measurments.view(dropPerSide, l - dropPerSide)
    }
  }

}
