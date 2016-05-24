package io.suggest.model.es

import scala.collection.AbstractSeq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.05.16 14:04
  * Description: Модель контейнера результатов dynSearch или какого-то иного поиска.
  * ElasticSearch возвращает результаты в виде Array[] или java-коллекций, что обычно не удобно или не оптимально.
  */

abstract class SearchRespT[T] extends AbstractSeq[T] with IndexedSeq[T] {

  /** Общее кол-во результатов по запросу. O(1). */
  def total: Long

}


/** Реализация [[SearchRespT]] для списка id. */
abstract class IdsSearchRespT
  extends SearchRespT[String]
