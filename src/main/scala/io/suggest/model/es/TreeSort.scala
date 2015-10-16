package io.suggest.model.es

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:33
 * Description:
 */
object TreeSort {

  // TODO спилить это. Используется только в seqTreeSort.
  implicit def listCmpOrdering[T <: Comparable[T]] = new ListCmpOrdering[T]

  /**
   * Сортировка последовательности экземпляров ES-модели с учетом древовидности через parent-child связи.
   * Это сравнительно ресурсоёмкая функция, поэтому использовать её следует только для административных задач,
   * например отображения полного списка категорий.
   * @param elems Исходные элементы в неопределённом порядке.
   * @tparam T Тип элементов.
   * @return Новая последовательности элементов и их уровней в дереве (level, T) в порядке для отображения дерева.
   */
  def seqTreeSort[T <: TreeSortable](elems: Seq[T]): Seq[(Int, T)] = {
    // Самый простой метод для сортировки: сгенерить List[String]-ключи на основе цепочек id и всех parent_id, затем отсортировать.
    val srcMap = elems.iterator.map { e => e.idOrNull -> e }.toMap
    def collectParentsRev(eOpt: Option[T], acc:List[String]): List[String] = {
      eOpt match {
        case Some(e) =>
          val acc1 = e.name :: acc
          if (e.parentId.isDefined) {
            val parentEOpt = srcMap get e.parentId.get
            collectParentsRev(parentEOpt, acc1)
          } else {
            acc1
          }

        case None => acc
      }
    }
    elems
      .map { e =>
        val eKey = collectParentsRev(Some(e), Nil)
        eKey -> e
      }
      .sortBy(_._1)
      .map { case (k, v) => k.size -> v }
  }

}


trait TreeSortable {
  def name: String
  def idOrNull: String
  def parentId: Option[String]
}


class ListCmpOrdering[T <: Comparable[T]] extends Ordering[List[T]] {
  @tailrec
  final def compare(x: List[T], y: List[T]): Int = {
    if (x.isEmpty) {
      -1
    } else if (y.isEmpty) {
      1
    } else {
      val headCmpResult = x.head compareTo y.head
      if (headCmpResult == 0) {
        compare(x.tail, y.tail)
      } else {
        headCmpResult
      }
    }
  }
}
