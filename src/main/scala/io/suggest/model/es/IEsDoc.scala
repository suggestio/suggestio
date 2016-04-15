package io.suggest.model.es

import io.suggest.util.JacksonWrapper
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.09.15 15:35
 * Description: TypeClass для доступа к общим, но неинтерфейсным полям, ES-результатов.
 */
object IEsDoc {

  /** Эксрактор для посковых результатов (/_search). */
  implicit val esHitEv = new IEsDoc[SearchHit] {

    override def rawVersion(v: SearchHit): Long = {
      v.getVersion
    }

    override def idOrNull(v: SearchHit): String = {
      v.getId
    }

    override def bodyAsString(v: SearchHit): String = {
      v.getSourceAsString
    }
    override def bodyAsScalaMap(v: SearchHit): collection.Map[String, AnyRef] = {
      v.getSource
    }
  }


  /** Экстрактор для GET by id результатов. */
  implicit val esGetRespEv = new IEsDoc[GetResponse] {

    override def rawVersion(v: GetResponse): Long = {
      v.getVersion
    }

    override def idOrNull(v: GetResponse): String = {
      v.getId
    }

    override def bodyAsString(v: GetResponse): String = {
      v.getSourceAsString
    }
    override def bodyAsScalaMap(v: GetResponse): collection.Map[String, AnyRef] = {
      v.getSourceAsMap
    }
  }



}


/** Интерфейс typeclass'ов. */
trait IEsDoc[-T] {

  def rawVersion(v: T): Long
  
  def version(v: T): Option[Long] = {
    val vraw = rawVersion(v)
    if (vraw < 0L) None else Some(vraw)
  }

  def idOrNull(v: T): String

  def id(v: T): Option[String] = {
    Option( idOrNull(v) )
  }

  /** Тривиальное извлечение данных через строку. */
  def bodyAsString(v: T): String

  /** legacy декодинг через jackson map. */
  def bodyAsScalaMap(v: T): collection.Map[String, AnyRef]

}
