package io.suggest.es.model

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.search.SearchHit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.09.15 15:35
 * Description: TypeClass для доступа к общим, но неинтерфейсным полям, ES-результатов.
 */
object IEsDoc {

  /** Эксрактор для посковых результатов (/_search). */
  implicit object EsSearchHitEv extends IEsDoc[SearchHit] {
    override def version(v: SearchHit): EsDocVersion =
      EsDocVersion.fromRawValues( v.getVersion, v.getSeqNo, v.getPrimaryTerm )
    override def idOrNull(v: SearchHit): String =
      v.getId
    override def bodyAsString(v: SearchHit): String =
      v.getSourceAsString
  }


  /** Экстрактор для GET by id результатов. */
  implicit object EsGetRespEv extends IEsDoc[GetResponse] {
    override def version(v: GetResponse): EsDocVersion =
      EsDocVersion.fromRawValues( v.getVersion, v.getSeqNo, v.getPrimaryTerm )
    override def idOrNull(v: GetResponse): String =
      v.getId
    override def bodyAsString(v: GetResponse): String =
      v.getSourceAsString
  }

}


/** Интерфейс typeclass'ов. */
trait IEsDoc[-T] {

  def version(v: T): EsDocVersion

  def idOrNull(v: T): String

  def id(v: T): Option[String] =
    Option( idOrNull(v) )

  /** Тривиальное извлечение данных через строку. */
  def bodyAsString(v: T): String

}
