package io.suggest.es.model

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.search.SearchHit

import scala.jdk.CollectionConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.09.15 15:35
 * Description: TypeClass для доступа к общим, но неинтерфейсным полям, ES-результатов.
 */
object IEsDoc {

  /** Эксрактор для посковых результатов (/_search). */
  implicit object EsSearchHitEv extends IEsDoc[SearchHit] {
    override def rawVersion(v: SearchHit): Long =
      v.getVersion
    override def idOrNull(v: SearchHit): String =
      v.getId
    override def bodyAsString(v: SearchHit): String =
      v.getSourceAsString
    override def bodyAsScalaMap(v: SearchHit): collection.Map[String, AnyRef] =
      v.getSourceAsMap.asScala
  }


  /** Экстрактор для GET by id результатов. */
  implicit object EsGetRespEv extends IEsDoc[GetResponse] {
    override def rawVersion(v: GetResponse): Long =
      v.getVersion
    override def idOrNull(v: GetResponse): String =
      v.getId
    override def bodyAsString(v: GetResponse): String =
      v.getSourceAsString
    override def bodyAsScalaMap(v: GetResponse): collection.Map[String, AnyRef] =
      v.getSourceAsMap.asScala
  }

}


/** Интерфейс typeclass'ов. */
trait IEsDoc[-T] {

  def rawVersion(v: T): Long

  def version(v: T): Option[Long] = {
    val vraw = rawVersion(v)
    Option.unless( vraw < 0L )(vraw)
  }

  def idOrNull(v: T): String

  def id(v: T): Option[String] =
    Option( idOrNull(v) )

  /** Тривиальное извлечение данных через строку. */
  def bodyAsString(v: T): String

  /** legacy декодинг через jackson map. */
  def bodyAsScalaMap(v: T): collection.Map[String, AnyRef]

}
