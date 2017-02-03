package io.suggest.es.model

import com.sksamuel.elastic4s.{HitAs, RichSearchHit, SearchDefinition}
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
  def as(from: SearchHit): To
}

/** Интерфейс typeclass'а для десериализатора RichSearchHit ответов elastic4s. */
trait IEs4sHitMapper[To] extends HitAs[To]

trait IEs4sJavaHitMapper[To] extends IEsHitMapper[To] with IEs4sHitMapper[To] {
  override def as(from: RichSearchHit): To = {
    as( from.java )
  }
}

trait IEsSourcingHelper[To] extends IEs4sHitMapper[To] {

  /** Подготовка search definition'а к будущему запросу. */
  def prepareSearchDef(searchDef: SearchDefinition): SearchDefinition = {
    searchDef
  }

}

class IdsSourcingHelper extends IEsSourcingHelper[String] {

  /** Подготовка search definition'а к будущему запросу. */
  override def prepareSearchDef(searchDef: SearchDefinition): SearchDefinition = {
    super.prepareSearchDef(searchDef)
      .fetchSource(false)
  }

  override def as(from: RichSearchHit): String = {
    from.id
  }

}
