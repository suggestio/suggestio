package io.suggest.es.model

import scala.collection.AbstractSeq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.05.16 14:04
  * Description: Модель контейнера результатов dynSearch или какого-то иного поиска.
  * ElasticSearch возвращает результаты в виде Array[] или java-коллекций, что обычно не удобно или не оптимально.
  */

trait ISearchResp[T] extends Seq[T] {

  /** Общее кол-во результатов по запросу. O(1). */
  def total: Long

}


/** Класс модели, ориентированный на search hits в виде массива. */
abstract class AbstractSearchResp[T]
  extends AbstractSeq[T]
  with IndexedSeq[T]      // TODO Надо как-то осилить IndexedSeqOptimized.
  with ISearchResp[T]

