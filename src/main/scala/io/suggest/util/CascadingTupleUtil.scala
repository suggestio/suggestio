package io.suggest.util

import cascading.tuple.Tuple

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.14 17:21
 * Description:
 */
object CascadingTupleUtil {

  /**
   * Десериализовать scala-карту из кортежа.
   * @param tuple результат convertMapToTuple
   * @return исходная immutable-карта.
   */
  @unchecked
  def convertTupleToMap(tuple: Tuple): Map[String, String] = {
    val iter = tuple.iterator()
    var acc: List[(String, String)] = Nil
    while (iter.hasNext) {
      val key = iter.next.asInstanceOf[String]
      val value = iter.next.asInstanceOf[String]
      acc = (key, value) :: acc
    }
    acc.toMap
  }

  /**
   * Сериализовать карту в Tuple.
   * @param map scala-карта.
   * @return Tuple.
   */
  def convertMapToTuple(map: Map[String, String]): Tuple = {
    val result = new Tuple()
    if (map != null) {
      map.foreach { case (k, v) =>
        result.add(k)
        result.add(v)
      }
    }
    result
  }

}
