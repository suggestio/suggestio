package io.suggest.index_info

import org.joda.time.LocalDate
import io.suggest.model.{MDVISubshard, SioSearchContext, MVirtualIndex}
import org.apache.hadoop.fs.Path
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.client.Client
import org.elasticsearch.action.search.SearchResponse
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.SiobixFs.dkeyPathConf
import io.suggest.util.SioEsIndexUtil.{SCROLL_TIMEOUT_INIT_DFLT, SCROLL_PER_SHARD_DFLT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.07.13 16:15
 * Description: Интерфейсы для системы индексов.
 */

// Базовый интерфейс для классов, исповедующих доступ к dkey-индексам.
trait MDVIUnit {
  val dkey: String
  val vin: String
  def subshards: List[MDVISubshard]
  val generation: Int
  def id: String
  def isSingleShard: Boolean
  def getAllTypes: List[String]
  def getAllTypesForShard(shardN: Int): List[MDVISubshard]
  def getVirtualIndex: MVirtualIndex
  def getTypesForRequest(sc:SioSearchContext): List[String]
  def save: Future[MDVIUnit]
  def getShards: Seq[String]
  def serialize: Array[Byte]

  def startFullScroll(timeout:TimeValue = SCROLL_TIMEOUT_INIT_DFLT, sizePerShard:Int = SCROLL_PER_SHARD_DFLT)(implicit client:Client): Future[SearchResponse]
}

// Трейт для индексного юнита, над которым можно проводить операции. Над merge-индексами например нельзя проводить
// ряд операций, ибо они целиком виртуальны, и используют данные других индексов.
trait MDVIUnitAlterable extends MDVIUnit {
  def setMappings(failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean]
  def deleteMappings(implicit client:Client, executor:ExecutionContext): Future[Unit]
  def getSubshardForDate(d:LocalDate): MDVISubshard
  def getInxTypeForDate(d:LocalDate): (String, String)

  /**
   * Бывает, что можно удалить всё вместе с физическим индексом. А бывает, что наоборот.
   * Тут функция, которая делает либо первое, либо второе в зависимости от обстоятельств.
   */
  def deleteIndexOrMappings(implicit client:Client, executor:ExecutionContext): Future[Unit]
}


object MDVIUnit {

  // Имя поддиректории модели в папке $dkey. Используется как основа для всего остального в этой модели.
  val rootDirNamePath = new Path("indexing")

  /**
   * Выдать путь к index-конфигам для указанного домена.
   * @param dkey ключ домена
   * @return
   */
  def getDkeyPath(dkey:String) = new Path(dkeyPathConf(dkey), rootDirNamePath)

}