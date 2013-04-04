package io.suggest.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 19:26
 * Description: Функции для работы со списками, множествами и т.д.
 */
object Lists {

  /**
   * Вывернуть карту k-[v] наизнанку. Т.е. v - список/множество.
   * [{a,[1,2,3]}, {b,[1,5,7]}, ...]  =>  [{1,[a,b]}, {2,[b]}, ...]
   * @param source исходная мапа, подлежащая выворачиванию.
   */
  def insideOut[K,V](source:Map[K, Traversable[V]]) : Map[V, Set[K]] = {
    // разворачиваем исходный список в [{1,a}, {2,a}, .., {1,b}, ...]
    val flatVKList = source.foldLeft(List[(V,K)]()) { case (_acc, (_k, _vl)) =>
      _vl.foldLeft(_acc) { case (__acc, __v) =>  (__v, _k) :: __acc }
    }
    // Используем groupBy и потом причесываем результат. Не самое оптимальное решение.
    flatVKList
      .groupBy(_._1)
      .map { case (v, lvk) => (v, lvk.map(_._2).toSet) }
  }


  /**
   * Замержить словари с обработкой значений при коллизии ключей. В scala почему-то нет этой очень очевидной функции.
   * Аналог dict:merge/3 в erlang.
   * @param ms Мапа для мержа.
   * @param f Функция обработки коллизий ключей, совпадает MergeF в dict:merge/3: (K,V1,V2) => V
   */
  def mergeMaps[K,V](ms: Map[K, V] *)(f: (K,V,V) => V) : Map[K, V] = {
    (Map[K, V]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(kv._1, a(kv._1), kv._2) else kv)
    }
  }


}
