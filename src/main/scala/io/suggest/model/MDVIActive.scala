package io.suggest.model

import org.joda.time.LocalDate
import io.suggest.util.{JacksonWrapper, LogsImpl, SioFutureUtil}
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.{Client => EsClient}
import io.suggest.index_info.MDVIUnitAlterable
import org.elasticsearch.common.unit.TimeValue
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import org.apache.hadoop.hbase.client.{Put, Scan, Get}
import org.apache.hadoop.hbase.HColumnDescriptor
import MObject.{hclient, CF_DINX_ACTIVE}
import scala.collection.JavaConversions._
import HTapConversionsBasic._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 */

object MDVIActive {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Ключи для сериализованной карты данных.
  val SER_KEY_GENERATION = "g"
  val SER_KEY_SUBSHARDS = "s"

  /** Десериализовать набор байт в MDVIActive.
   * @param b Массив байтов.
   * @return Экземпляр MDVIActive.
   */
  implicit def deserializeBytes(dkey:String, vin:String, b: Array[Byte]): MDVIActive = {
    val is = new ByteArrayInputStream(b)
    val data = try {
      JacksonWrapper.deserialize[Map[String, Any]](is)
    } finally {
      is.close()
    }
    val generation = data(SER_KEY_GENERATION).asInstanceOf[Int]
    val subshardsInfo = JacksonWrapper.convert [List[MDVISubshardInfo]] (data(SER_KEY_SUBSHARDS))
    MDVIActive(dkey=dkey, vin=vin, generation=generation, subshardsInfo=subshardsInfo)
  }


  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param vin vin, определяющий шардинг виртуального индекса.
   * @return Опциональный сабж.
   */
  def getForDkeyVin(dkey:String, vin:String): Future[Option[MDVIActive]] = {
    // TODO Задействовать асинхронный клиент при появлении возможности, ибо тут адски блокирующаяся поделка.
    val column: Array[Byte] = vin
    val getReq = new Get(dkey)
      .addColumn(CF_DINX_ACTIVE, column)
    hclient map { _client =>
      try {
        val results = _client.get(getReq)
        if (results.isEmpty) {
          None
        } else {
          val bytes = results.getColumnLatest(CF_DINX_ACTIVE, column).getValue
          Some(deserializeBytes(dkey=dkey, vin=vin, b=bytes))
        }
      } finally {
        _client.close()
      }
    }
  }


  /** Найти все dkey, которые используют указанный индекс.
   * Ресурсоемкая операция, т.к. для этого нужно просмотреть все dkey.
   * @param vin имя индекса, для которого проводится поиск.
   * @return Список dkey->MDVIActive без повторяющихся элементов в произвольном порядке.
   *         При желании можно сконвертить в карту через .toMap()
   */
  def getAllLatestForVin(vin: String): Future[Iterable[MDVIActive]] = {
    // TODO Тут очень нужен неблокирующий асинхронный hbase-клиент.
    trace("getAllForVin(%s)" format vin)
    val column: Array[Byte] = vin
    // Скан всех рядов, содержащих колонку с названием индеска с указанной CF.
    val scanReq = new Scan().addColumn(CF_DINX_ACTIVE, column)
    hclient map { _client =>
      try {
        val scanner = _client.getScanner(scanReq)
        try {
          scanner map { result =>
            val v = result.getColumnLatest(CF_DINX_ACTIVE, column).getValue
            val dkey = result.getRow
            deserializeBytes(vin=vin, dkey=dkey, b=v)
          }

        } finally {
          scanner.close()
        }
      } finally {
        _client.close()
      }
    }
  }


  /** Асинхронно прочитать все активные индексы для указанного dkey. Быстрая операция.
   * @param dkey Ключ домена.
   * @return Будущий список MDVIActive.
   */
  def getAllForDkey(dkey: String): Future[Iterable[MDVIActive]] = {
    val getReq = new Get(dkey).addFamily(CF_DINX_ACTIVE)
    hclient map { _client =>
      try {
        _client
          .get(getReq)
          .getFamilyMap(CF_DINX_ACTIVE)
          .map { case(vinBytes, dataBytes) => deserializeBytes(dkey=dkey, vin=vinBytes, b=dataBytes) }

      } finally {
        _client.close()
      }
    }
  }


  /** Выдать CF-дескриптор для используемого CF
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF_DINX_ACTIVE).setMaxVersions(3)

}


import MDVIActive._

/**
 * Класс уровня домена. Хранит информацию по виртуальному индексу в контексте текущего dkey.
 * @param dkey Ключ домена
 * @param vin Имя нижележащего виртуального индекса.
 * @param subshardsInfo Список подшард.
 * @param generation Поколение. При миграции в другой индекс поколение инкрементируется.
 */
case class MDVIActive(
  dkey:       String,
  vin:        String,
  generation: Int = 0,
  subshardsInfo: List[MDVISubshardInfo] = List(new MDVISubshardInfo(0))

) extends MDVIUnitAlterable with Serializable {

  import LOGGER._

  /**
   * Отразить список подшард в виде полноценных объектов Subshard, ссылкающихся на своего родителя.
   * @return Список MDVISubshard.
   */
  @JsonIgnore
  def subshards: List[MDVISubshard] = subshardsInfo.map(new MDVISubshard(this, _))

  /**
   * Выдать id этого виртуального индекса.
   * @return
   */
  @JsonIgnore
  def id: String = "%s$%s$%s" format (dkey, vin, generation)

  /**
   * Нужно сохранить документ. И встает вопрос: в какую именно подшарду.
   * @param d дата документа.
   * @return подшарду для указанной даты. Из неё можно получить название типа при необходимости.
   */
  def getSubshardForDate(d:LocalDate): MDVISubshard = {
    val days = toDaysCount(d)
    getSubshardForDaysCount(days)
  }

  /**
   * Выдать шарду для указанного кол-ва дней.
   * @param daysCount кол-во дней.
   * @return название шарды.
   */
  def getSubshardForDaysCount(daysCount:Int): MDVISubshard = {
    isSingleShard match {
      case true  => subshards.head

      // TODO Выбираются все шарды слева направо, однако это может неэффективно.
      //      Следует отбрасывать "свежие" шарды слева, если их многовато.
      case false => subshards find { _.subshardData.lowerDateDays < daysCount } getOrElse subshards.last
    }
  }

  /**
   * Когда настало время сохранять что-то, то нужно выбрать шарду подходящую.
   * @param d дата документа
   * @return кортеж индекса и типа.
   */
  def getInxTypeForDate(d:LocalDate): (String, String) = {
    val days = toDaysCount(d)
    val ss = getSubshardForDaysCount(days)
    val inx = ss.getShardForDaysCount(days)
    inx -> ss.getTypename
  }

  @JsonIgnore
  lazy val isSingleShard = subshards.length == 1

  /**
   * Выдать все типы, относящиеся ко всем индексам в этой подшарде.
   * @return список типов.
   */
  @JsonIgnore
  def getAllTypes = subshards.map(_.getTypename)

  /**
   * Выдать все типы, лежащие на указанной шарде.
   * @param shardN номер родительской шарды
   * @return списочек подшард
   */
  def getAllTypesForShard(shardN: Int): List[MDVISubshard] = {
    subshards.foldLeft[List[MDVISubshard]] (Nil) { (acc, ss) =>
      if (ss.subshardData.shards contains shardN) {
        ss :: acc
      } else {
        acc
      }
    }
  }

  /**
   * Выдать экземпляр модели MVirtualIndex. Линк между моделями по ключу.
   * Считаем, что вирт.индекс точно существует, если существует зависимый от него экземпляр сабжа.
   */
  @JsonIgnore
  lazy val getVirtualIndex = MVirtualIndex(vin)


  /**
   * Вебморда собирается сделать запрос, и ей нужно узнать, какие индесы и какие типы в них необходимо
   * опрашивать.
   * @param sc Контекст поискового запроса.
   * @return Список названий индексов/шард и список имен типов в этих индексах.
   */
  def getTypesForRequest(sc: SioSearchContext): List[String] = {
    error("getTypesForRequest: NOT YET IMPLEMENTED")
    getAllTypes  // TODO нужно как-то что-то где-то определять.
  }

  def exportInternalState: Map[String, Any] = Map(
    SER_KEY_GENERATION -> generation,
    SER_KEY_SUBSHARDS  -> subshardsInfo
  )

  /**
   * Сохранить текущий экземпляр в хранилище.
   * @return Сохраненные (т.е. текущий) экземпляр сабжа.
   */
  @JsonIgnore
  def save: Future[MDVIActive] = {
    val v = JacksonWrapper.serialize(exportInternalState)
    val putReq = new Put(dkey).add(CF_DINX_ACTIVE, vin, v)
    hclient map { _client =>
      try {
        _client.put(putReq)
        this
      } finally {
        _client.close()
      }
    }
  }

  /** Сериализовать этот экземпляр класса в десериализуемое представление.
   * @return Строка, но по идее должны быть байты.
   */
  @JsonIgnore
  def serialize: Array[Byte] = {
    val os = new ByteArrayOutputStream(128)
    try {
      JacksonWrapper.serialize(os, this)
    } finally {
      os.close()
    }
    os.toByteArray
  }

  /**
   * Выдать натуральные шарды натурального индекса, обратившись к вирт.индексу.
   * @return Список названий индексов.
   */
  @JsonIgnore
  def getShards = getVirtualIndex.getShards

  /**
   * Выставить маппинги для всех подшард.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return true, если всё хорошо.
   */
  def setMappings(failOnError:Boolean = true)(implicit esClient:EsClient, ec:ExecutionContext): Future[Boolean] = {
    debug("setMappings(%s): starting" format failOnError)
    // Запустить парралельно загрузку маппингов для всех типов (всех подшард).
    Future.traverse(subshards) {
      _.setMappings(getShards, failOnError)
    } flatMap { boolSeq =>
      // Проанализировать результаты.
      // TODO Возможно, тут проверка излишня, и при любой ошибке будет хороший failed future автоматом.
      val boolSeqDistinct = boolSeq.distinct
      val isEverythingOk  = boolSeqDistinct.length == 1 && boolSeqDistinct.head
      if (isEverythingOk || !failOnError) {
        Future.successful(isEverythingOk)
      } else {
        val msg = "Failed to setMappings on one or more subshards. %s => %s" format (subshards, boolSeq)
        error(msg)
        Future.failed(new RuntimeException(msg))
      }
    }
  }

  /**
   * Удалить маппинги позволяет удалить данные.
   * @return Выполненный фьючерс когда всё закончится.
   */
  def deleteMappings(implicit esClient:EsClient, ec:ExecutionContext): Future[Unit] = {
    debug("deleteMappings(): dkey=%s vin=%s types=%s" format (dkey, vin, subshards map { _.getTypename }))
    SioFutureUtil
      .mapLeftSequentally(subshards, ignoreErrors=true) { _.deleteMappaings() }
      .map(_ => Unit)
  }

  /**
   * Используется ли указанный vin другими доменами?
   * @return true, если используется.
   */
  def isVinUsedByOtherDkeys(implicit ec:ExecutionContext): Future[Boolean] = {
    getAllLatestForVin(vin) map { vinUsedBy =>
      var l = vinUsedBy.size
      if (vinUsedBy contains dkey)
        l -= 1
      val result = l > 0
      debug(
        if (result)
          "VIndex is used by several domains: %s." format vinUsedBy
        else
          "VIndex is only used by me."
      )
      result
    }
  }


  /**
   * Бывает, что можно удалить всё вместе с физическим индексом. А бывает, что наоборот.
   * Тут функция, которая делает либо первое, либо второе в зависимости от обстоятельств.
   */
  def deleteIndexOrMappings(implicit esClient: EsClient, ec: ExecutionContext): Future[Unit] = {
    isVinUsedByOtherDkeys map {
      case true  => deleteMappings
      case false => getVirtualIndex.eraseShards
    }
  }


  import io.suggest.util.SioEsIndexUtil._

  /**
   * Запуск полного скроллинга по индексу.
   * @param timeout Время жизни курсора на стороне ES.
   * @param sizePerShard Сколько брать из шарды за один шаг.
   * @return Фьючес ответа, содержащего скроллер и прочую инфу.
   */
  def startFullScroll(timeout:TimeValue = SCROLL_TIMEOUT_INIT_DFLT, sizePerShard:Int = SCROLL_PER_SHARD_DFLT)(implicit esClient:EsClient) = {
    startFullScrollIn(getShards, getAllTypes, timeout, sizePerShard)
  }

}

