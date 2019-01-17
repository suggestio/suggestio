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

  /** Сборка сорсера только id'шников. */
  implicit def idsHelper: IEsSourcingHelper[String] = {
    new IEsSourcingHelper[String] {

      override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
        super.prepareSrb(srb)
          .setFetchSource(false)
      }

      override def mapSearchHit(from: SearchHit): String = {
        from.getId
      }

    }
  }

}


/** Интерфейс SourcingHelper'а.
  * Такие typeclass'ы модифицируют поисковые запросы и их результаты. */
trait IEsSourcingHelper[To] extends IEsSrbMutator {

  override def toString: String = try {
    getClass.getSimpleName
  } catch { case _: Throwable =>
    super.toString
  }

  /** Десериализатор SearchHit ответов elasticsearch. */
  def mapSearchHit(from: SearchHit): To

}


trait IEsSrbMutator {

  /** Подготовка search definition'а к будущему запросу. */
  def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
    srb
  }

}
