package io.suggest.util

import collection.mutable
import scala.annotation.tailrec

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


  /**
   * Тоже самое для mutable-словаря. Стоит заменить это добро на нормальный вызов с collections.MapLike и манифестами.
   * @return
   */
  def mergeMutableMaps[K,V](ms:mutable.Map[K,V] *)(f: (K,V,V) => V) : mutable.Map[K,V] = {
    (mutable.Map[K, V]() /: (for (m <- ms; kv <- m) yield kv)) { (a, kv) =>
      a + (if (a.contains(kv._1)) kv._1 -> f(kv._1, a(kv._1), kv._2) else kv)
    }
  }


  /** Получение n-ного хвоста от списка. Если длина списка недостаточна, то будет ошибка.
    * Функция не генерирует мусора, и аналогична erlang lists:nthtail/2.
    * @param l Исходный список.
    * @param nth Сколько хвоство скинуть.
    * @tparam T Тип элемента, для самой функции значения никакого не имеет.
    * @return Один из хвостов списка.
    */
  @tailrec def nthTail[T](l: List[T], nth:Int): List[T] = {
    if (nth > 0) {
      nthTail(l.tail, nth - 1)
    } else {
      l
    }
  }


  /**
   * Поиск общего хвоста между двумя списками одинаковой длины.
   * Если длина списков отличается, то будет IllegialArgumentException.
   * @param l1 Один список.
   * @param l2 Другой список.
   * @tparam T Тип элементов в списках.
   * @return Общий хвост. Если такого нет, то будет Nil. Если длины списков разные, то IllegialArgumentException.
   */
  def getCommonTail[T](l1:List[T], l2:List[T]): List[T] = {
    if (l1 == l2) {
      l1
    } else if (l1.isEmpty || l2.isEmpty) {
      throw new IllegalArgumentException("List arguments must have same length. Rests are: " + l1 + " and " + l2)
    } else {
      getCommonTail(l1.tail, l2.tail)
    }
  }



  /**
   * Поиск наибольшей общей непрервыной под-последовательности между двумя списками.
   * @param a Список.
   * @param b Список.
   * @tparam T Тип элементов списков.
   * @return Наибольшая общая под-последовательность или Nil, если ничего не найдено.
   *         Если найденная подпоследовательность состоит из одного элемента, то тоже будет Nil.
   */
  def findLCS[T](a: List[T], b: List[T]): List[T] = {
    val aLen = a.size
    val bLen = b.size
    if (aLen >= bLen) {
      findLCS1(a, b, bLen)
    } else {
      findLCS1(b, a, aLen)
    }
  }

  /**
   * Найти наибольшую непрерывную общую под-последовательность между двумя указанными списками.
   * Если longer-список короче shorter, то результат считается недостоверным.
   * @param longer Длинный список.
   * @param shorter Короткий список.
   * @param shorterSize Текущая длина короткого списка.
   * @tparam T Тип элемента списков.
   * @return Наибольшая общая подпоследовательность в исходном порядке.
   *         Если найденная подпоследовательность состоит из одного элемента, то тоже будет Nil.
   */
  def findLCS1[T](longer:List[T], shorter:List[T], shorterSize:Int): List[T] = {
    val minSliceLen = 2
    // Отбрасываем головы от shorter-списка (без мусора) и дергаем containsSlice().
    @tailrec def shortenHeads(slice:List[T], sliceLen:Int): List[T] = {
      if (sliceLen >= minSliceLen) {
        if (longer containsSlice slice) {
          slice
        } else {
          shortenHeads(slice.tail, sliceLen - 1)
        }
      } else {
        Nil
      }
    }
    // Отбрасываем элементы из хвоста shorten-списка. Отбрасывание из хвоста порождает много мусора, поэтому эта функция вызывается реже.
    @tailrec def shortenTails(slice0: List[T], slice0sz:Int, acc0:List[T] = Nil, acc0len:Int = 0): List[T] = {
      if (slice0sz >= acc0len && slice0sz >= minSliceLen) {
        val lcs = shortenHeads(slice0, slice0sz)
        val maybeLcs: List[T] = if (lcs.isEmpty) {
          // Текущий слайс и его обрубки (слева) не содержаться в исходном списке. Надо укоротить хвост и попробовать снова.
          null
        } else {
          // Слайс или его обрубок совпадает.
          val lcsSz = lcs.size
          if (acc0len < lcsSz) {
            // Слайс или его обрубок длинее текущего аккамулятора. Значит его и возвращаем.
            lcs
          } else {
            // Слайс-обрубок коротковат. Возможно, отброс элемента из хвоста поможет найти
            null
          }
        }
        // Чтобы не порождать лишний мусор и не писать код рекурсивного вызова дважды, используем null как флаг для рекурсии.
        if (maybeLcs == null) {
          val sz1 = slice0sz - 1
          shortenTails(slice0.slice(0, sz1), sz1)
        } else {
          maybeLcs
        }
      } else {
        // Любые последовательности будут уже короче, поэтому дальше нет смысла дергать проверки слайсов.
        acc0
      }
    }
    // Начать сверку.
    shortenTails(shorter, shorter.size)
  }



  /**
   * Построение наибольшего общего под-списка среди двух списков (массиво, т.к. нужно random access к данным).
   * Если в последовательности есть разрывы, то они перепрыгиваются (игнорируется).
   * Взято из [[http://www.cs.cityu.edu.hk/~lwang/cs5302/LCS.java LCS.java]].
   * Рекурсивное палево из [[https://stackoverflow.com/q/5734020 SO/5734020]] обходим стороной, ибо бесконечный цикл.
   * @param x Массив.
   * @param y Массив.
   * @tparam T Тип элементов массивов.
   * @return Непрерывная общая под-последовательность элементов или Nil, если ничего общего не найдено.
   */
  def findRaggedLCS[T](x: Array[T], y:Array[T]): List[T] = {
    var i = 0
    var j = 0
    /* initialize the n x m matrix B and C for dynamic programming
     * B[i][j] stores the directions, C[i][j] stores the length of LCS of
     * X[0..i-1] and Y[0..j-1]
    */
    val n = x.length
    val m = y.length
    val c = Array.ofDim[Int](n+1, m+1)  //  int[][] C = new int[n+1][m+1];
    val b = Array.ofDim[Int](n+1, m+1)

    /* C[i][0] = 0 for 0<=i<=n */
    for (i <- 0 to n) {
      c(i)(0) = 0
    }

    /* C[0][j] = 0 for  0<=j<=m */
    for (j <- 0 to m) {
      c(0)(j) = 0
    }

    /* dynamic programming */
    for (i <- 1 to n) {
      for (j <- 1 to m) {
        if (x(i-1) == y(j-1)) {
          c(i)(j) = c(i-1)(j-1) + 1
          b(i)(j) = 1    /* diagonal */
        } else if (c(i-1)(j) >= c(i)(j-1)) {
          c(i)(j) = c(i-1)(j)
          b(i)(j) = 2;   /* down */
        } else {
          c(i)(j) = c(i)(j-1)
          b(i)(j) = 3;   /* forword */
        }
      }
    }
    /* Backtracking */
    var lcs: List[T] = Nil
    i = n
    j = m
    while (i!=0 && j!=0) {
      if (b(i)(j) == 1) {   /* diagonal */
        lcs ::= x(i-1)
        i = i - 1
        j = j - 1
      }
      if (b(i)(j) == 2) {   /* up */
        i = i - 1
      }

      if (b(i)(j) == 3) {   /* backword */
        j = j - 1
      }
    }

    lcs
  }


}
