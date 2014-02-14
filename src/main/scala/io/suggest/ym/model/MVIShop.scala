package io.suggest.ym.model

import io.suggest.model._
import cascading.tuple.{Fields, TupleEntry, Tuple}
import org.elasticsearch.client.Client
import scala.concurrent.{Future, ExecutionContext}
import com.scaleunlimited.cascading.BaseDatum
import io.suggest.util.{SioModelUtil, SerialUtil}
import io.suggest.model.SioHBaseAsyncClient._
import org.hbase.async.GetRequest
import scala.collection.JavaConversions._
import io.suggest.ym.index.YmIndex
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.02.14 13:41
 * Description: Под-индекс для магазина. Фактически, это просто тип в индексе ТЦ [[MVIMart]].
 * Имя типа соответствует имени магазина. Поле vin'а дублируется из вышестоящего индекса ТЦ.
 * Ключ ряда состоит только из shop_id.
 */
object MVIShop extends MVIUnitStatic[MVIShop] {

  val SHOP_ID_FN = fieldName("shopId")
  val MART_ID_FN = fieldName("martId")
  val FIELDS = new Fields(SHOP_ID_FN, MART_ID_FN, VIN_FN, GENERATION_FN)

  /** Префикс ключа ряда в HBase. */
  val ROW_KEY_PREFIX = "$"

  val TYPENAME_MART_SHOP_SEP = "-"

  /** Текущие тип+версия сериализации. Используется для дефолтового [[isSerVsnMatches]], выкинуть можно в любое время. */
  override val SER_VSN = 500.toShort

  /** Сериализовать id магазина в строку ключа ряда. */
  def shopId2rowKeyStr(shopId: Int): String = ROW_KEY_PREFIX + shopId

  /** Сериализовать id магазина в ключ ряда. */
  def shopId2rowKey(shopId: Int): Array[Byte] = shopId2rowKeyStr(shopId).getBytes

  /**
   * Сериализовать экземпляр сабжа в экземпляр данных, пригодных для отправки в HBase.
   * @param mviShop Экземпляр [[MVIShop]].
   * @return Экземпляр [[MVIUnitHBaseSerialized]].
   */
  def serializeForHBase(mviShop: MVIShop): MVIUnitHBaseSerialized = {
    val value = new Tuple
    value addShort SER_VSN
    value addLong mviShop.generation
    MVIUnitHBaseSerialized(
      rowkey    = shopId2rowKey(mviShop.shopId),
      qualifier = MVIUnit.serializeVin(mviShop.vin),
      value     = SerialUtil.serializeTuple(value)
    )
  }

  
  
  /** Десериализовать исходную запись из HBase.
    * @param rowkey Ключ ряда.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIShop]].
    */
  def deserializeRaw(rowkey:AnyRef, qualifier:AnyRef, value:Array[Byte]): MVIShop = {
    val shopId = deserializeRowKey2ShopId(rowkey)
    deserializeWithShopId(shopId, qualifier, value=value)
  }

  /** Десериализовать данные после хранения в HBase. На случай, если dkey уже десериализован.
    * @param shopId id магазина.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIShop]].
    */
  def deserializeWithShopId(shopId: Int, qualifier:AnyRef, value:Array[Byte]): MVIShop = {
    val vin = MVIUnit.deserializeQualifier2Vin(qualifier)
    deserializeWithShopIdVin(shopId=shopId, vin=vin, value=value)
  }

  /** Десериализовать данные при условии, что vin уже десериализован.
   * @param rowkey Ключ ряда.
   * @param vin Строка виртуального индекса.
   * @param value Значение ячейки.
   * @return Экземпляр [[MVIShop]].
   */
  def deserializeWithVin(rowkey:AnyRef, vin:String, value:Array[Byte]): MVIShop = {
    val shopId = deserializeRowKey2ShopId(rowkey)
    deserializeWithShopIdVin(shopId=shopId, vin=vin, value=value)
  }


  /** Десериализации при наличии готовых vin и dkey.
    * @param shopId id торгового центра.
    * @param vin Строка виртуального индекса.
    * @param value Значение ячейки.
    * @return Экземпляр [[MVIShop]].
    */
  def deserializeWithShopIdVin(shopId:Int, vin:String, value:Array[Byte]): MVIShop = {
    val tuple = SerialUtil.deserializeTuple(value)
    finalDeserializer(shopId, vin, tuple)
  }


  /** Этот десериализатор вызывается из [[MVIUnit.deserializeRaw]] при наличии частично-десериализованных данных. */
  override def deserializeSemiRaw(rowkey: Array[Byte], vin: String, value: Tuple, serVsn: Short): MVIShop = {
    val shopId = deserializeRowKey2ShopId(rowkey)
    finalDeserializer(shopId, vin, value, serVsn)
  }


  def finalDeserializer(shopId: Int, vin: String, value: Tuple): MVIShop = {
    val serVsn = value getShort 0
    finalDeserializer(shopId, vin, value, serVsn)
  }
  def finalDeserializer(shopId: Int, vin: String, value: Tuple, serVsn:Short): MVIShop = {
    serVsn match {
      case SER_VSN =>
        val generation = value getLong 1
        new MVIShop(shopId=shopId, vin=vin, generation=generation)
    }
  }


  /**
   * Десериализация ключа ряда.
   * @param rowkey Сырой ключ ряда.
   * @return Целочисленное shop_id.
   */
  def deserializeRowKey2ShopId(rowkey: AnyRef) : Int = {
    val rkStr = SioModelUtil.deserialzeHCellCoord(rowkey)
    if (rkStr startsWith ROW_KEY_PREFIX) {
      rkStr.substring(1).toInt
    } else {
      throw new IllegalArgumentException("Cannot parse row key: unexpected key prefix: " + rkStr)
    }
  }



  /**
   * Выдать все данные по индексам для указанного ТЦ.
   * @param shopId id торгового центра.
   * @return Список индексов ТЦ в неопределённом порядке.
   */
  def getForShopId(shopId: Int)(implicit ec: ExecutionContext): Future[List[MVIShop]] = {
    val rowkey = shopId2rowKey(shopId)
    val getReq = new GetRequest(HTABLE_NAME, rowkey).family(CF)
    ahclient.get(getReq) map { results =>
      results.foldLeft[List[MVIShop]] (Nil) { (acc, kv) =>
        val mviShop = deserializeWithShopId(shopId=shopId, qualifier=kv.qualifier, value=kv.value)
        mviShop :: acc
      }
    }
  }

  /**
   * Выдать точное значение ячейки из хранилища.
   * @param shopId id торгового центра.
   * @param vin Идентификатор индекса.
   * @return Распарсенное значение ячейки, если есть.
   */
  def getForMartIdVin(shopId: Int, vin: String)(implicit ec: ExecutionContext): Future[Option[MVIShop]] = {
    val rowkey = shopId2rowKey(shopId)
    val qualifier = MVIUnit.serializeVin(vin)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF)
      .qualifier(qualifier)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val kv = results.head
        val mvim = deserializeWithShopIdVin(shopId, vin, kv.value)
        Some(mvim)
      }
    }
  }

  override def isMyType(obj: MVIUnit): Boolean = obj.isInstanceOf[MVIShop]
}


import MVIShop._

class MVIShop extends BaseDatum(FIELDS) with MVIUnit {

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(shopId: Int, vin: String, generation: Long = MVIUnit.GENERATION_BOOTSTRAP) = {
    this()
    this.shopId = shopId
    this.vin = vin
    this.generation = generation
  }

  lazy val mviMart = MVIMart.getForMartIdVin(martId, vin).map(_.get)

  /** Указатель на объект-компаньон, чтобы получить доступ к статическим данным модели. */
  override def companion = MVIShop

  def shopId = _tupleEntry.getInteger(SHOP_ID_FN)
  def shopId_=(shopId: Int) = _tupleEntry.setInteger(SHOP_ID_FN, shopId)

  def martId = _tupleEntry.getInteger(MART_ID_FN)
  def martId_=(martId: Int) = _tupleEntry.setInteger(MART_ID_FN, martId)

  /** Узнать, не используется ли текущий индекс другими субъектами.
    * Нет, т.к. тут только маппинг только для одного магазина. */
  override def isVinUsedByOthers(implicit esClient: Client, ec: ExecutionContext): Future[Boolean] = {
    mviMart flatMap { _.isVinUsedByOthers }
  }

  /** Эквивалент toString, выдающий максимально короткую строку. Используется для некоторых задач логгинга. */
  override def toShortString = s"shop/$shopId/$vin"

  /** Сериализовать данные из кортежа в ключ-колонку-значение для HBase. */
  override def serializeForHbase = MVIShop.serializeForHBase(this)

  /** Выдать реальный ключ ряда в таблице хранилища. */
  override def getRowKey: Array[Byte] = shopId2rowKey(shopId)

  /** Имя типа, в котором хранятся все товары магазина. */
  val typename = martId.toString + TYPENAME_MART_SHOP_SEP + shopId

  /** В каких типа индекса производить поиск? */
  override def getTypesForRequest(sc: SioSearchContext): List[String] = {
    List(typename)
  }

  /** Выставить маппинг полей для типа-магазина. */
  override def setMappings(failOnError: Boolean)(implicit client: Client, executor: ExecutionContext): Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(getShards : _*)
      .setType(typename)
      .setSource(YmIndex.getIndexMapping(typename))
      .execute()
      .map { result => true }
  }

  /** Удаление маппинга магазина. Обычно происходит при удалении магазина. */
  override def deleteMappings(implicit client: Client, executor: ExecutionContext): Future[_] = {
    client.admin().indices()
      .prepareDeleteMapping(getShards : _*)
      .setType(typename)
      .execute()
  }

  /** Бывает, что можно удалить всё вместе с физическим индексом. А бывает, что наоборот.
    * Но в случае с магазином - внешний индекс ТЦ отделён от метаданных индекса магазина. */
  override def eraseIndexOrMappings(implicit esClient: Client, ec: ExecutionContext) = deleteMappings

}

