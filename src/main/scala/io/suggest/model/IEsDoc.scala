package io.suggest.model

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


  /** Mock-адаптер для тестирования сериализации-десериализации моделей на базе play.json.
    * На вход он получает просто экземпляры классов моделей. */
  implicit def mockPlayDocRespEv = new IEsDoc[EsModelCommonT] {
    override def id(v: EsModelCommonT): Option[String] = {
      v.id
    }
    override def version(v: EsModelCommonT): Option[Long] = {
      v.versionOpt
    }
    override def rawVersion(v: EsModelCommonT): Long = {
      v.versionOpt.getOrElse(-1)
    }
    override def bodyAsScalaMap(v: EsModelCommonT): collection.Map[String, AnyRef] = {
      JacksonWrapper.convert[collection.Map[String, AnyRef]]( v.toJson )
    }
    override def bodyAsString(v: EsModelCommonT): String = {
      v.toJson
    }
    override def idOrNull(v: EsModelCommonT): String = {
      v.idOrNull
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
