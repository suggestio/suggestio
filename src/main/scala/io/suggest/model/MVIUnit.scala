package io.suggest.model

import org.elasticsearch.client.Client
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{CascadingFieldNamer, SioModelUtil, MacroLogsImpl}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hbase.async.{DeleteRequest, PutRequest}
import SioHBaseAsyncClient._
import org.apache.hadoop.hbase.HColumnDescriptor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.07.13 16:15
 * Description: Интерфейсы для системы виртуальных индексов.
 */

object MVIUnit {

  def CF = MObject.CF_MVI

  // Короткие подсказки для модели и её внешних пользователей, которым не важно внутреннее устройство модели.
  def HTABLE_NAME = MObject.HTABLE_NAME
  def HTABLE_NAME_BYTES = MObject.HTABLE_NAME_BYTES


  /** Десериализовать hbase qualifier в строку vin.
    * @param q Квалификатор (колонка) ячейки в каком-то неопределенном формате.
    * @return Строка, использованная в качестве значения квалификатора.
    */
  def deserializeQualifier2Vin(q: AnyRef) = SioModelUtil.deserialzeHCellCoord(q)

  /** Сериализовать vin. По сути просто конвертится строка в байты.
   * @param vin Строка виртуального индекса.
   * @return Байты, пригодные для задание qualifier'a.
   */
  def serializeVin(vin: String): Array[Byte] = SioModelUtil.serializeStrForHCellCoord(vin)


  /** Выдать CF-дескриптор для используемого CF
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF).setMaxVersions(3)


  // Номер поколения - это миллисекунды, вычтенные и деленные на указанный делитель.
  // Номер поколения точно не будет раньше этого времени. Выкидываем.
  // После запуска в продакшен, менять эти цифры уже нельзя (понадобится полное удаление всех данных по индексам вместе с оными).
  val GENERATION_SUBSTRACT  = 1381140627123L // 2013/oct/07 14:11 +04:00.
  val GENERATION_RESOLUTION = 10000L         // Округлять мс до 10 секунд.

  /** Это поколение выставляется для сабжа нулевого поколения, т.е. если ещё не было ребилдов. */
  val GENERATION_BOOTSTRAP = -1

  /**
   * Сгенерить id текущего поколения с точностью до 10 секунд.
   * @return Long.
   */
  def currentGeneration: Long  = (System.currentTimeMillis() - GENERATION_SUBSTRACT) / GENERATION_RESOLUTION

  /**
   * Перевести поколение во время в миллисекундах с учетом точности/округления.
   * @param g Генерейшен, произведенный ранее функцией [[currentGeneration]].
   * @return Число миллисекунд с 1970/1/1 0:00:00 округленной согласно [[GENERATION_SUBSTRACT]].
   */
  def generationToMillis(g: Long): Long = g * GENERATION_RESOLUTION + GENERATION_SUBSTRACT

}


trait MVIUnitStatic extends CascadingFieldNamer {

  def CF = MVIUnit.CF
  def HTABLE_NAME = MVIUnit.HTABLE_NAME
  def HTABLE_NAME_BYTES = MVIUnit.HTABLE_NAME_BYTES

  val VIN_FN        = fieldName("vin")
  val GENERATION_FN = fieldName("generation")
}


import MVIUnit._

// Базовый интерфейс для классов, исповедующих доступ к dkey-индексам.
trait MVIUnit extends MacroLogsImpl {
  import LOGGER._

  /** Выдать реальный ключ ряда в таблице хранилища. */
  @JsonIgnore
  def getRowKey: Array[Byte]

  /** Выдать ключ ряда в таблице в виде строки. */
  @JsonIgnore
  def getRowKeyStr: String = new String(getRowKey)

  @JsonIgnore
  def vin: String

  @JsonIgnore
  def generation: Long
  
  /** Эквивалент toString, выдающий максимально короткую строку. Используется для некоторых задач логгинга. */
  @JsonIgnore
  def toShortString: String

  @JsonIgnore
  def isSingleShard: Boolean

  @JsonIgnore
  def getAllTypes: List[String]

  /**
   * Выдать экземпляр модели MVirtualIndex. Линк между моделями по ключу.
   * Считаем, что вирт.индекс автоматически существует, если существует зависимый от него экземпляр сабжа.
   */
  @JsonIgnore
  def getVirtualIndex = MVirtualIndex(vin)

  def getTypesForRequest(sc: SioSearchContext): List[String]

  /**
   * Сохранить текущий экземпляр в хранилище.
   * @return Сохраненные (т.е. текущий) экземпляр сабжа.
   */
  def save(implicit ec:ExecutionContext): Future[_] = {
    val ser = serializeForHbase
    val putReq = new PutRequest(HTABLE_NAME_BYTES, ser.rowkey, CF.getBytes, ser.qualifier, ser.value)
    ahclient.put(putReq)
  }


  /**
   * Выдать натуральные шарды натурального индекса, обратившись к вирт.индексу.
   * @return Список названий индексов.
   */
  @JsonIgnore
  def getShards = getVirtualIndex.getShards


  def setMappings(failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean]
  def deleteMappings(implicit client:Client, executor:ExecutionContext): Future[Unit]

  /** Узнать, не используется ли текущий индекс другими субъектами.
    * В случае доменного мультииндекса - другими доменами. */
  def isVinUsedByOthers(implicit esClient: Client, ec:ExecutionContext): Future[Boolean]

  /**
   * Бывает, что можно удалить всё вместе с физическим индексом. А бывает, что наоборот.
   * Тут функция, которая делает либо первое, либо второе в зависимости от обстоятельств.
   */
  def eraseIndexOrMappings(implicit esClient: Client, ec: ExecutionContext): Future[_] = {
    val logPrefix = "deleteIndexOrMappings():"
    isVinUsedByOthers flatMap {
      case true  =>
        warn(s"$logPrefix vin=$vin used by dkeys, other than '$getRowKeyStr'. Delete mapping...")
        deleteMappings

      case false => eraseBackingIndex
    }
  }


  /** Удалить весь индекс целиком, даже если в нём хранятся другие сайты (без проверок). */
  def eraseBackingIndex(implicit esClient: Client, ec:ExecutionContext): Future[Boolean] = {
    warn(s"eraseBackingIndex(): vin=$vin dkey='$getRowKeyStr' Deleting real index FULLY!")
    getVirtualIndex.eraseShards
  }

  @JsonIgnore
  def getQualifier: Array[Byte] = serializeVin(vin)

  @JsonIgnore
  def serializeForHbase: MVIUnitHBaseSerialized


  /**
   * Удалить ряд из хранилища. Проверку по generation не делаем, ибо оно неразрывно связано с dkey+vin и является
   * вспомогательной в рамках flow величиной.
   * @return Фьючерс для синхронизации.
   */
  def delete(implicit ec: ExecutionContext): Future[_] = {
    val delReq = new DeleteRequest(HTABLE_NAME_BYTES, getRowKey, CF.getBytes, getQualifier)
    ahclient.delete(delReq)
  }

}


/**
 * Контейнер для набора, пригодного для немедленного сохранения в HBase.
 * @param rowkey Ключ ряда.
 * @param qualifier Имя колонки (qualifier).
 * @param value Байты payload'а.
 */
case class MVIUnitHBaseSerialized(rowkey:Array[Byte], qualifier:Array[Byte], value:Array[Byte])

