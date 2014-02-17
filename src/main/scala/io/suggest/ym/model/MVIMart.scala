package io.suggest.ym.model

import org.elasticsearch.client.Client
import scala.concurrent.{Future, ExecutionContext}
import cascading.tuple.{Tuple, TupleEntry, Fields}
import com.scaleunlimited.cascading.BaseDatum
import io.suggest.util.{SioModelUtil, SerialUtil}
import org.hbase.async.GetRequest
import scala.collection.JavaConversions._
import io.suggest.model.{MVIUnitStatic, SioSearchContext, MVIUnit}
import io.suggest.model.SioHBaseAsyncClient._
import io.suggest.model.MVIUnitHBaseSerialized
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.02.14 16:47
 * Description: MVIMart - реализация MVIUnit, используемая для хранения информации об индексе торгового центра.
 * Для ключа ряда используется однобайтовый префикс чтобы все MVI одной категории хранились вместе.
 */
object MVIMart extends MVIUnitStatic[MVIMart] {

  /** Префикс */
  val ROW_KEY_PREFIX = "!"

  val MART_ID_FN = fieldName("martId")
  val FIELDS = new Fields(MART_ID_FN, VIN_FN, GENERATION_FN)

  val SER_VSN = 200.toShort

  /** Сериализовать id торгового центра в строку ключа ряда. */
  def martId2rowKeyStr(martId: Int): String = ROW_KEY_PREFIX + martId

  /** Сериализовать id торгового центра в ключ ряда. */
  def martId2rowKey(martId: Int): Array[Byte] = martId2rowKeyStr(martId).getBytes

  /**
   * Сериализовать для HBase экземпляр сабжа.
   * @param mvim Экземпляр сабжа.
   * @return Сериализованный набор данных, готовый к отправке в HBase.
   */
  def serializeForHBase(mvim: MVIMart): MVIUnitHBaseSerialized = {
    val value = new Tuple
    value addShort SER_VSN
    value addLong mvim.generation
    MVIUnitHBaseSerialized(
      rowkey    = martId2rowKey(mvim.martId),
      qualifier = MVIUnit.serializeVin(mvim.vin),
      value     = SerialUtil.serializeTuple(value)
    )
  }


  /**
   * Десериализация ключа ряда.
   * @param rowkey Сырой ключ ряда.
   * @return Целочисленное mart_id.
   */
  def deserializeRowKey2MartId(rowkey: AnyRef) : Int = {
    val rkStr = SioModelUtil.deserialzeHCellCoord(rowkey)
    if (rkStr startsWith ROW_KEY_PREFIX) {
      rkStr.substring(1).toInt
    } else {
      throw new IllegalArgumentException("Cannot parse row key: unexpected key prefix: " + rkStr)
    }
  }


  /** Десериализовать исходную запись из HBase.
    * @param rowkey Ключ ряда.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIMart]].
    */
  def deserializeRaw(rowkey:AnyRef, qualifier:AnyRef, value:Array[Byte]): MVIMart = {
    val martId = deserializeRowKey2MartId(rowkey)
    deserializeWithMartId(martId, qualifier, value=value)
  }

  /** Десериализовать данные после хранения в HBase. На случай, если dkey уже десериализован.
    * @param martId id торгового центра.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIMart]].
    */
  def deserializeWithMartId(martId: Int, qualifier:AnyRef, value:Array[Byte]): MVIMart = {
    val vin = MVIUnit.deserializeQualifier2Vin(qualifier)
    deserializeWithMartIdVin(martId=martId, vin=vin, value=value)
  }

  /** Десериализовать данные при условии, что vin уже десериализован.
   * @param rowkey Ключ ряда.
   * @param vin Строка виртуального индекса.
   * @param value Значение ячейки.
   * @return Экземпляр [[MVIMart]].
   */
  def deserializeWithVin(rowkey:AnyRef, vin:String, value:Array[Byte]): MVIMart = {
    val martId = deserializeRowKey2MartId(rowkey)
    deserializeWithMartIdVin(martId=martId, vin=vin, value=value)
  }


  /** Десериализации при наличии готовых vin и dkey.
    * @param martId id торгового центра.
    * @param vin Строка виртуального индекса.
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIMart]].
    */
  def deserializeWithMartIdVin(martId:Int, vin:String, value:Array[Byte]): MVIMart = {
    val tuple = SerialUtil.deserializeTuple(value)
    finalDeserializer(martId, vin, tuple)
  }


  /** Этот десериализатор вызывается из [[MVIUnit.deserializeRaw]] при наличии частично-десериализованных данных. */
  override def deserializeSemiRaw(rowkey: Array[Byte], vin: String, value: Tuple, serVsn: Short): MVIMart = {
    val martId = deserializeRowKey2MartId(rowkey)
    finalDeserializer(martId, vin, value, serVsn)
  }

  def finalDeserializer(martId: Int, vin: String, value: Tuple): MVIMart = {
    val serVsn = value getShort 0
    finalDeserializer(martId, vin, value, serVsn)
  }
  def finalDeserializer(martId: Int, vin: String, value: Tuple, serVsn:Short): MVIMart = {
    serVsn match {
      case SER_VSN =>
        val generation = value getLong 1
        new MVIMart(martId=martId, vin=vin, generation=generation)
    }
  }


  /**
   * Выдать все данные по индексам для указанного ТЦ.
   * @param martId id торгового центра.
   * @return Список индексов ТЦ в неопределённом порядке.
   */
  def getForMartId(martId: Int)(implicit ec: ExecutionContext): Future[List[MVIMart]] = {
    val rowkey = martId2rowKey(martId)
    val getReq = new GetRequest(HTABLE_NAME, rowkey).family(CF)
    ahclient.get(getReq) map { results =>
      results.foldLeft[List[MVIMart]] (Nil) { (acc, kv) =>
        val mvim = deserializeWithMartId(martId=martId, qualifier=kv.qualifier, value=kv.value)
        mvim :: acc
      }
    }
  }

  /**
   * Выдать точное значение ячейки из хранилища.
   * @param martId id торгового центра.
   * @param vin Идентификатор индекса.
   * @return Распарсенное значение ячейки, если есть.
   */
  def getForMartIdVin(martId: Int, vin: String)(implicit ec: ExecutionContext): Future[Option[MVIMart]] = {
    val rowkey = martId2rowKey(martId)
    val qualifier = MVIUnit.serializeVin(vin)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF)
      .qualifier(qualifier)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val kv = results.head
        val mvim = deserializeWithMartIdVin(martId, vin, kv.value)
        Some(mvim)
      }
    }
  }

  override def isMyType(obj: MVIUnit): Boolean = obj.isInstanceOf[MVIMart]
}


import MVIMart._

class MVIMart extends BaseDatum(FIELDS) with MVIUnit {

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(martId: Int, vin:String, generation:Long = MVIUnit.GENERATION_BOOTSTRAP) = {
    this()
    this.martId = martId
    this.vin = vin
    this.generation = generation
  }


  def martId = _tupleEntry getInteger MART_ID_FN
  def martId_=(martId: Int) = _tupleEntry.setInteger(MART_ID_FN, martId)

  override def companion = MVIMart

  /** Выдать ключ ряда в таблице в виде строки. */
  override def getRowKeyStr: String = martId2rowKeyStr(martId)

  /** Выдать реальный ключ ряда в таблице хранилища. */
  override def getRowKey: Array[Byte] = martId2rowKey(martId)

  def serializeForHbase: MVIUnitHBaseSerialized = MVIMart.serializeForHBase(this)

  /** Узнать, не используется ли текущий индекс другими субъектами?
    * Нужно найти всех, кто использует этот индекс (vin) и проверить. */
  override def isVinUsedByOthers(implicit esClient: Client, ec: ExecutionContext) = {
    Future successful false
  }

  /**
   * Удалить маппинги. Т.к. вызов относится к ТЦ, который хранит весь индекс целиком, то удалять надо все маппинги.
   * Удаление всех маппингов из индекса не поддерживается, т.к. в этом нет смысла. Либо удалять индекс, либо не удалять вообще.
   * @return Всегда [[UnsupportedOperationException]].
   */
  override def deleteMappings(implicit client: Client, executor: ExecutionContext): Future[_] = {
    throw new UnsupportedOperationException(s"deleteMappings() for full mart index never be implemented, because it have no sense.")
  }

  /** Выставить маппинги. */
  override def setMappings(failOnError: Boolean)(implicit client: Client, executor: ExecutionContext): Future[Boolean] = {
    // TODO Выставлять маппинг для поиска по сайту ТЦ. Можно делать поиск только через _all.
    Future successful true
  }

  /** В каких типах индекса искать? Обычно - во всех. */
  override def getTypesForRequest(sc: SioSearchContext): List[String] = {
    // TODO Может быть стоит как-то учитывать специализацию запроса и ограничивать поиск по типам?
    Nil
  }

  /** Эквивалент toString, выдающий максимально короткую строку. Используется для некоторых задач логгинга. */
  override def toShortString: String = s"mart/$martId/$vin"

  override def toString: String = s"${getClass.getSimpleName}($martId, $vin, $generation)"
}
