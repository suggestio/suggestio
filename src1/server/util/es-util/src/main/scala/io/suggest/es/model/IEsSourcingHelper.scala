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

  /** Cорсер только id'шников. */
  implicit object IdsHelper extends IEsSourcingHelper[String] {

    override def prepareSrb(srb: SearchRequestBuilder): SearchRequestBuilder = {
      super.prepareSrb(srb)
        .setFetchSource(false)
    }

    override def mapSearchHit(from: SearchHit): String = {
      from.getId
    }

  }


  /** Сорсер SearchHit'ов. */
  implicit object SearchHitSH extends IEsSourcingHelper[SearchHit] {
    override def mapSearchHit(from: SearchHit) = from
  }

}


/** Интерфейс SourcingHelper'а.
  * Такие typeclass'ы модифицируют поисковые запросы и их результаты. */
// TODO Надо явно разделить два этих трейта, иначе нельзя реализовать source-хелперы под id и под полный инстанс.
//      И тогда для source[] видимо надо два типа: промежуточный невозвращаемый для prepareSrb(), и результирующий для SearchHit-маппера.
//      Вообще, надо SearchHit-маппинг унести куда-нибудь максимально в статику и максимально абстрактно для всего, а не только для source()-метода.
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
