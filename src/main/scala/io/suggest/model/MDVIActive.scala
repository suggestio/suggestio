package io.suggest.model

import org.joda.time.LocalDate
import io.suggest.util.{JacksonWrapper, LogsImpl, SioFutureUtil}
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{Promise, ExecutionContext, Future}
import org.elasticsearch.client.{Client => EsClient}
import io.suggest.index_info.MDVIUnitAlterable
import org.elasticsearch.common.unit.TimeValue
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import org.apache.hadoop.hbase.HColumnDescriptor
import scala.collection.JavaConversions._
import HTapConversionsBasic._
import SioHBaseAsyncClient._
import org.hbase.async.{DeleteRequest, PutRequest, KeyValue, GetRequest}
import scala.util.{Failure, Success}
import java.util.{ArrayList => juArrayList}
import cascading.tuple.Tuple

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 *
 * TODO В этой модели исторически живут два вида сериализации: кусочная в json и полноразмерная в Tuple.
 *      Надо устранить зоопарк сериализаций. При чтении из бд внутрь flow json-представление конвертируется в Tuple-представление.
 *      Tuple-представление быстрое и вроде бы универсальное, однако вопрос эволюции его во времени пока не решен.
 *
 *      Вариант: перевести MDVIActive в подкласс BaseDatum (выковырять из bixo), добавить версии,
 *      а subshardsInfo сериализовывать в подкортеж, как это сделано в моделях bixo. Для tap'ов использовать простую
 *      tuple-схему.
 */
object MDVIActive {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Короткие подсказки для модели и её внешних пользователей, которым не важно внутреннее устройство модели.
  def HTABLE_NAME = MObject.HTABLE_NAME
  def CF = MObject.CF_DINX_ACTIVE

  // Ключи для сериализованной карты данных.
  val SER_KEY_GENERATION = "g"
  val SER_KEY_SUBSHARDS = "s"

  // Номер поколения - это миллисекунды, вычтенные и деленные на указанный делитель.
  // Номер поколения точно не будет раньше этого времени. Выкидываем.
  val GENERATION_SUBSTRACT  = 1381140627123 // 2013/oct/07 14:11 +04:00.
  val GENERATION_RESOLUTION = 10000         // Округлять мс до 10 секунд.

  /** Это поколение выставляется для сабжа, если  */
  val GENERATION_BOOTSTRAP = -1

  /** Десериализовать набор байт в MDVIActive.
   * @param dkey Ключ домена, ибо массив байт обычно не содержит dkey.
   * @param vin Id индекса. Обычно является ключом и хранится вне массива байтов.
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
  def getForDkeyVin(dkey:String, vin:String)(implicit ec:ExecutionContext): Future[Option[MDVIActive]] = {
    val column: Array[Byte] = vin
    val getReq = new GetRequest(HTABLE_NAME, dkey).family(CF).qualifier(column)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val bytes = results.head.value()
        Some(deserializeBytes(dkey=dkey, vin=vin, b=bytes))
      }
    }
  }


  /** Найти все dkey, которые используют указанный индекс. Неблокирующая непоточная операция, завершается лишь
   * только когда всё-всё готово.
   * Ресурсоемкая операция, т.к. для этого нужно просмотреть все dkey.
   * @param vin имя индекса, для которого проводится поиск.
   * @return Список dkey->MDVIActive без повторяющихся элементов в произвольном порядке.
   *         При желании можно сконвертить в карту через .toMap()
   */
  def getAllLatestForVin(vin: String)(implicit ec:ExecutionContext): Future[List[MDVIActive]] = {
    trace("getAllForVin(%s)" format vin)
    val column: Array[Byte] = vin
    val scanner = ahclient.newScanner(HTABLE_NAME)
    scanner.setFamily(CF)
    scanner.setQualifier(column)
    val p = Promise[List[MDVIActive]]()
    def foldNextAsync(_acc: List[MDVIActive], _fut: Future[juArrayList[juArrayList[KeyValue]]]) {
      _fut onComplete {
        case Success(null) => p success _acc

        case Success(rows) =>
          val acc1 = rows.foldLeft(_acc) { (__acc, __rows) =>
            __rows.foldLeft(__acc) { (___acc, row) =>
              val dkey = row.key()
              deserializeBytes(vin=vin, dkey=dkey, b=row.value) :: ___acc
            }
          }
          foldNextAsync(acc1, scanner.nextRows)

        case Failure(ex) => p failure ex
      }
    }
    foldNextAsync(Nil, scanner.nextRows)
    p.future
  }


  /**
   * Прочитать всю CF из таблицы.
   * @return Будущий список MDVIActive.
   */
  def getAll(implicit ec:ExecutionContext): Future[List[MDVIActive]] = {
    val scanner = ahclient.newScanner(HTABLE_NAME)
    scanner.setFamily(CF)
    val p = Promise[List[MDVIActive]]()
    def foldNextAsync(acc0: List[MDVIActive], fut0: Future[juArrayList[juArrayList[KeyValue]]]) {
      fut0 onComplete {
        case Success(null) => p success acc0

        case Success(rows) =>
          val acc1 = rows.foldLeft(acc0) { (_acc0, _rows) =>
            _rows.foldLeft(_acc0) { (__acc0, row) =>
              val dkey = row.key()
              val vin = row.qualifier()
              deserializeBytes(vin=vin, dkey=dkey, b=row.value) :: __acc0
            }
          }
          foldNextAsync(acc1, scanner.nextRows)

        case Failure(ex) => p failure ex
      }
    }
    foldNextAsync(Nil, scanner.nextRows)
    p.future
  }


  /** Асинхронно прочитать все активные индексы для указанного dkey. Быстрая операция.
   * @param dkey Ключ домена.
   * @return Будущий список MDVIActive.
   */
  def getAllForDkey(dkey: String)(implicit ec:ExecutionContext): Future[Seq[MDVIActive]] = {
    val getReq = new GetRequest(HTABLE_NAME, dkey).family(CF)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        Nil
      } else {
        results.map { kv => deserializeBytes(dkey=dkey, vin=kv.key, b=kv.value) }
      }
    }
  }


  /** Выдать CF-дескриптор для используемого CF
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF).setMaxVersions(3)


  /** Сериализовать данные в tuple.
   * @param dvi исходные экземпляр сабжа, подлежащий сериализации в переносимый tuple.
   * @return Tuple, НЕ содержащий dkey.
   */
  def serializeToDkeylessTuple(dvi: MDVIActive): Tuple = {
    val t = new Tuple(dvi.vin)
    t.addLong(dvi.generation)
    MDVISubshard.serializeSubshardInfo(dvi.subshardsInfo, t)
  }


  /** Десериализация данных, сгенерированных в serializeToDkeylessTuple() и dkey
   * @param dkey Ключ домена.
   * @param t Кортеж.
   * @return Экземпляр MDVIActive.
   */
  def deserializeFromTupleDkey(dkey: String, t: Tuple): MDVIActive = {
    val vin :: generation :: sii = t.toList
    val si = MDVISubshard.deserializeSubshardInfo(sii)
    MDVIActive(dkey, vin.toString, generation.toString.toInt, si)
  }


  /**
   * Сгенерить id текущего поколения с точностью до 10 секунд.
   * @return Long.
   */
  def currentGeneration: Long  = (System.currentTimeMillis() - GENERATION_SUBSTRACT) / GENERATION_RESOLUTION

  /**
   * Перевести поколение во время в миллисекундах с учетом точности/округления.
   * @param g Генерейшен, произведенный ранее функцией currentGeneration().
   * @return Число миллисекунд с 1970/1/1 0:00:00 округленной согласно GENERATION_SUBSTRACT.
   */
  def generationToMillis(g: Long): Long = g * GENERATION_RESOLUTION + GENERATION_SUBSTRACT


  /** Дефолтовое значение поля subshardsInfo. */
  val SUBSHARD_INFO_DFLT = List(new MDVISubshardInfo(0))

}


import MDVIActive._

/**
 * Класс уровня домена. Хранит информацию по виртуальному индексу в контексте текущего dkey.
 * @param dkey Ключ домена
 * @param vin Имя нижележащего виртуального индекса.
 * @param generation Поколение. При миграции в другой индекс поколение увеличивается.
 * @param subshardsInfo Список подшард. "Новые" всегла левее (ближе к head), чем старые. В последнем хвосте списка
 *                      всегда находится шарда с lowerDaysCount = 0.
 */
case class MDVIActive(
  dkey:       String,
  vin:        String,
  generation: Long,
  subshardsInfo: List[MDVISubshardInfo] = MDVIActive.SUBSHARD_INFO_DFLT

) extends MDVIUnitAlterable with Serializable {

  if (subshardsInfo.isEmpty) {
    throw new IllegalArgumentException("subshardInfo MUST contain at least one shard. See default value for example.")
  }


  import LOGGER._

  protected implicit def toSubshard(sInfo: MDVISubshardInfo) = new MDVISubshard(this, sInfo)
  protected implicit def toSubshardsList(sInfoList: List[MDVISubshardInfo]) = sInfoList.map(toSubshard)

  /**
   * Отразить список подшард в виде полноценных объектов Subshard, ссылкающихся на своего родителя.
   * @return Список MDVISubshard.
   */
  @JsonIgnore
  def subshards: List[MDVISubshard] = subshardsInfo

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
      case false =>
        val si = subshardsInfo find { _.lowerDateDays < daysCount } getOrElse subshardsInfo.last
        si
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
    error("getTypesForRequest: STUB: NOT YET IMPLEMENTED")
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
  def save(implicit ec: ExecutionContext): Future[MDVIActive] = {
    val v = JacksonWrapper.serialize(exportInternalState)
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], dkey:Array[Byte], CF, vin:Array[Byte], v:Array[Byte])
    ahclient.put(putReq) map { _ => this }
  }

  /**
   * Удалить ряд из хранилища.
   * @return Фьючерс для синхронизации.
   */
  def delete(implicit ec: ExecutionContext): Future[Unit] = {
    val delReq = new DeleteRequest(HTABLE_NAME, dkey)
    ahclient.delete(delReq) map { _ => () }
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
        if (result) {
          "VIndex is used by several domains: %s." format vinUsedBy
        } else {
          "VIndex is only used by me."
        }
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

  /** Сериализовать в cascading.tuple без dkey.
   * @return cascading Tuple, содержащий все необходимые для восстановления данные за искл. поля dkey.
   */
  def toDkeylessTuple = serializeToDkeylessTuple(this)
}

