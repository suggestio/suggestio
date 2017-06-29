package io.suggest.es.model

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.SearchHit

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 12:35
  * Description: Интерфейс для хелперов typeclass'ов, обслуживающих нужды source() (akka.streams).
  */

object IEsSourcingHelper {

  implicit def idsHelper = new IdsSourcingHelper

}


/** Интерфейс typeclass'а для десериализатора SearchHit ответов elasticsearch. */
trait IEsHitMapper[To] {
  def mapSearchHit(from: SearchHit): To
}

trait IEsSrbMutator {

  /** Подготовка search definition'а к будущему запросу. */
  def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
    srb
  }

}


/** Интерфейс SourcingHelper'а.
  * Такие typeclass'ы модифицируют поисковые запросы и их результаты. */
trait IEsSourcingHelper[To] extends IEsHitMapper[To] with IEsSrbMutator {

  override def toString: String = try {
    getClass.getSimpleName
  } catch { case _: Throwable =>
    super.toString
  }

}


/** Сборка сорсера только id'шников. */
class IdsSourcingHelper extends IEsSourcingHelper[String] {

  override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
    super.prepareSrb(srb)
      .setFetchSource(false)
  }

  override def mapSearchHit(from: SearchHit): String = {
    from.getId
  }

}
