package io.suggest.common.coll

import japgolly.univeq.UnivEq

import scala.annotation.tailrec
import scala.collection.BuildFrom
import scala.language.higherKinds
import scala.reflect.ClassTag
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
  def insideOut[K,V](source: Map[K, Iterable[V]]): Map[V, Set[K]] = {
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
  def mergeMaps[K,V](ms: Map[K, V]*)(f: (K,V,V) => V): Map[K, V] = {
    (for (m <- ms; kv <- m) yield kv)
      .foldLeft( Map[K, V]() ) { (a, kv) =>
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
  @tailrec def nthTail[T](l: List[T], nth: Int): List[T] = {
    if (nth > 0) {
      nthTail(l.tail, nth - 1)
    } else {
      l
    }
  }

  /** Поиск общего префикса между двумя списками.
    *
    * @param l1 Список 1.
    * @param l2 Список 2.
    * @return Nil, если общего префикса нет.
    *         Иначе - наибольший общий префикс с исходном порядке.
    */
  def largestCommonPrefix[T: UnivEq](l1: List[T], l2: List[T]): List[T] =
    _largestCommonPrefixRevAcc(l1, l2, Nil)

  @tailrec private def _largestCommonPrefixRevAcc[T: UnivEq](
                                                              l1: List[T],
                                                              l2: List[T],
                                                              prefixRev0: List[T]): List[T] = {
    if (l1.nonEmpty && l2.nonEmpty && l1.head ==* l2.head) {
      _largestCommonPrefixRevAcc( l1.tail, l2.tail, l1.head :: prefixRev0 )
    } else {
      prefixRev0.reverse
    }
  }


  /**
   * Поиск общего хвоста между двумя списками одинаковой длины.
   * Если длина списков отличается, то будет IllegialArgumentException.
   * @param l1 Один список.
   * @param l2 Другой список.
   * @tparam T Тип элементов в списках.
   * @return Общий хвост.
   *         Если такого нет, то будет Some(Nil).
   *         Если длины списков различаются, то None.
   */
  @tailrec def largestCommonTailSameLen[T: UnivEq](l1: List[T], l2: List[T]): List[T] = {
    if (l1 ==* l2) {
      l1
    } else if (l1.isEmpty || l2.isEmpty) {
      throw new IllegalArgumentException("List arguments must have same length. Rests are: " + l1 + " and " + l2)
    } else {
      largestCommonTailSameLen(l1.tail, l2.tail)
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
  def largestCommonSeq[T](a: List[T], b: List[T]): List[T] = {
    val aLen = a.size
    val bLen = b.size
    if (aLen >= bLen) {
      largestCommonSeq1(a, b, bLen)
    } else {
      largestCommonSeq1(b, a, aLen)
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
  def largestCommonSeq1[T](longer: List[T], shorter: List[T], shorterSize: Int): List[T] = {
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
    @tailrec def shortenTails(slice0: List[T], slice0sz: Int, acc0: List[T] = Nil, acc0len: Int = 0): List[T] = {
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
  def raggedLargestCommonSeq[T: UnivEq](x: Array[T], y: Array[T]): List[T] = {
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
        if (x(i-1) ==* y(j-1)) {
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
      if (b(i)(j) ==* 1) {   /* diagonal */
        lcs ::= x(i-1)
        i = i - 1
        j = j - 1
      }
      if (b(i)(j) ==* 2) {   /* up */
        i = i - 1
      }

      if (b(i)(j) ==* 3) {   /* backword */
        j = j - 1
      }
    }

    lcs
  }


  /**
   * Нанооптимизация, возможная с некоторыми неизменяемыми коллекциями.
   * Метод нужен для объединения абстрактных scala-immutable-коллекций по О(1) или с минимальными затратами,
   * когда программист считает, что это вполе возможно.
   * А это возможно, когда коллекции являюстя stream'ами или списками.
   */
  def appendSeqHead[T](head: Seq[T], tail: Seq[T]): Seq[T] = {
    if (head.isEmpty) {
      tail
    } else if (head.length == 1) {
      // head очень короткий. Это значит можно попробовать кое-какие трюки.
      val el = head.head
      tail match {
        case l: List[T]   => el :: l
        case ll: LazyList[T] => el #:: ll
        case _            => _appendSeqMaybeStream(head, tail)
      }
    } else {
      // head не выглядит слишком коротким. Но всё-таки пытаемся провести через Stream.
      _appendSeqMaybeStream(head, tail)
    }
  }

  private def _appendSeqMaybeStream[T](head: Seq[T], tail: Seq[T]): Seq[T] = {
    head match {
      case ll: LazyList[T] =>
        ll appendedAll tail
      case _ =>
        head ++ tail
    }
  }


  /**
   * map + foldLeft с поддержкой BuildFrom.
   * @param src Исходная коллекция.
   * @param acc0 Исходный аккамулятор.
   * @param f Функция маппинга и сверстки.
   * @tparam A Тип исходного элемента.
   * @tparam Coll Тип коллекции. Исходной и финальной.
   * @tparam B Тип отмаппленного элемента.
   * @tparam Acc Тип аккамулятора.
   * @return Кортеж из аккамулятора и отмаппленной коллекции.
   */
  def mapFoldLeft[A, Coll[X] <: IterableOnce[X], B, Acc]
                 (src: Coll[A], acc0: Acc)
                 (f: (Acc, A) => (Acc, B))
                 (implicit cbf: BuildFrom[Coll[A], B, Coll[B]]): (Acc, Coll[B]) = {
    val seqb = cbf.newBuilder( src )
    val acc1 = src.iterator.foldLeft(acc0) { (_acc0, _el) =>
      val (_acc1, b) = f(_acc0, _el)
      seqb += b
      _acc1
    }
    (acc1, seqb.result())
  }


  /** Опционально добавить элемент во главу списка.
    *
    * @param xOpt Опциональный добавляемый элемент.
    * @param tail Хвост списка.
    * @tparam T Тип элементов всех участвующих коллекций.
    * @return Если xOpt == None, то tail.
    *         Если Some(x), то x::tail.
    */
  def prependOpt[T](xOpt: Option[T])(tail: List[T]): List[T] = {
    xOpt.fold(tail)(_ :: tail)
  }


  /** Аналог flatten для списка/коллекции, но в обратном порядке и без билдеров.
    * Это бывает эффективно на больших коллекциях, когда не важен порядок.
    */
  def flattenRev[T](elems: IterableOnce[IterableOnce[T]]): List[T] = {
    toListRev {
      elems
        .iterator
        .flatten
    }
  }


  /** Это как toList, но с выхлопом в обратном порядке.
    * Это позволяет обойтись без всяких CBF, с минимальным расходом памяти и
    * минимальной сложностью, т.е. O(N).
    * @param elems Исходная коллекция или итератор.
    * @return Список исходных элементов в обратном порядке.
    */
  def toListRev[T](elems: IterableOnce[T], acc0: List[T] = Nil): List[T] = {
    elems
      .iterator
      .foldLeft(acc0) { (acc, e) => e :: acc }
  }


  /** Быстро сравнить (eq) содержимое двух последовательных коллекций без учёта типов коллекций. */
  def isElemsEqs[T <: AnyRef: UnivEq](tr1: IterableOnce[T], tr2: IterableOnce[T]): Boolean = {
    isElemsEqsIter( tr1.iterator, tr2.iterator )
  }

  /** Пройти по двум итераторам, сравнивая элементы через eq. */
  @tailrec
  def isElemsEqsIter[T <: AnyRef: UnivEq](tr1: Iterator[T], tr2: Iterator[T]): Boolean = {
    val tr1HasNext = tr1.hasNext
    val tr2HasNext = tr2.hasNext
    if (tr1HasNext && tr2HasNext) {
      if ( tr1.next() ===* tr2.next() ) {
        // Идти дальше по итераторам
        isElemsEqsIter(tr1, tr2)
      } else {
        // Есть различающийся элемент.
        false
      }

    } else {
      // Один hasNext, другой !hasNext. Значит, длина итераторов различается => коллекции не равны. => false
      // Оба итератора закочились одновременно. Значит они равны. => true
      !(tr1HasNext || tr2HasNext)
    }
  }


  /** Вернуть функцию для flatMap(), которая будет возвращать только элементы указанного типа. */
  def ofTypeF[X, T <: X: ClassTag]: PartialFunction[X, List[T]] = {
    case t: T => t :: Nil
    case _    => Nil
  }


  object Implicits {

    implicit class ListExt[T](private val source: List[T]) extends AnyVal {

      /** O(n)-аналог List.span(), но true-аккамулятор возвращается в развёрнутом виде.
        * Т.е. [1,2,3,4,5,6] и {x<=3} вернёт результат ([3,2,1], [4,5,6]).
        *
        * @param f Предикат.
        * @return
        */
      def spanRev(f: T => Boolean): (List[T], List[T]) = {
        @tailrec
        def __doSpan(headAccRev: List[T], rest: List[T]): (List[T], List[T]) = {
          headAccRev match {
            case hd :: tl if f(hd) =>
              __doSpan(hd :: headAccRev, tl)
            case _ =>
              (headAccRev, rest)
          }
        }

        __doSpan(Nil, source)
      }

      def :?: (x: Option[T]): List[T] =
        x.fold(source)(_ :: source)

    }


    implicit class IterableOnceListsOps[T](private val source: IterableOnce[T]) extends AnyVal {

      /** Добавить все элементы этой коллекции в начало указанного списка в обратном порядке. */
      def prependRevTo(list: List[T]): List[T] = {
        toListRev(source, list)
      }

      /** Sub-optimal set creating, oriented to very small sets (~ 0-4 elements). */
      def toSmallSet: Set[T] = {
        source
          .iterator
          .foldLeft( Set.empty[T] )( _ + _ )
      }

    }


  }


}
