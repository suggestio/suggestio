package io.suggest.model

import org.elasticsearch.client.Client
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{SerialUtil, CascadingFieldNamer, SioModelUtil, MacroLogsImpl}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hbase.async.{KeyValue, DeleteRequest, PutRequest}
import SioHBaseAsyncClient._
import org.apache.hadoop.hbase.HColumnDescriptor
import cascading.tuple.{Tuple, TupleEntry}
import io.suggest.ym.model.{MVIShop, MVIMart}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.07.13 16:15
 * Description: Интерфейсы для системы виртуальных индексов.
 */

object MVIUnit extends MacroLogsImpl {

  import LOGGER._

  def CF = MObject.CF_MVI

  // Короткие подсказки для модели и её внешних пользователей, которым не важно внутреннее устройство модели.
  def HTABLE_NAME = MObject.HTABLE_NAME
  def HTABLE_NAME_BYTES = MObject.HTABLE_NAME_BYTES

  val MVI_COMPANIONS = List(MVIMart, MVIShop)

  def deserializeRaw(rowkey: Array[Byte], qualifier: Array[Byte], value: Array[Byte]): Option[MVIUnit] = {
    val vin = deserializeQualifier2Vin(qualifier)
    deserializeWithVin(rowkey, vin, value=value)
  }
  def deserializeWithVin(rowkey:Array[Byte], vin:String, value: Array[Byte]): Option[MVIUnit] = {
    val tuple = SerialUtil.deserializeTuple(value)
    val serVsn = tuple getShort 0
    MVI_COMPANIONS.find { _.isSerVsnMatches(serVsn) }
      .map { companion =>
        companion.deserializeSemiRaw(rowkey, vin, tuple, serVsn)
      }
  }

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


  /** Найти все dkey, которые используют указанный индекс. Неблокирующая непоточная операция, завершается лишь
   * только когда всё-всё готово.
   * Ресурсоемкая операция, т.к. для этого нужно просмотреть все dkey.
   * @param vin имя индекса, для которого проводится поиск.
   * @return Список (dkey, [[MDVIActive]]) без повторяющихся элементов в произвольном порядке.
   *         При желании можно сконвертить в карту через .toMap()
   */
  def getAllUnitsForVin(vin: String)(implicit ec:ExecutionContext): Future[List[MVIUnit]] = {
    trace(s"getAllForVin($vin): Starting...")
    val column: Array[Byte] = vin.getBytes
    val scanner = ahclient.newScanner(HTABLE_NAME)
    scanner.setFamily(CF)
    scanner.setQualifier(column)
    val folder = new AsyncHbaseScannerFold[List[MVIUnit]] {
      def fold(acc: List[MVIUnit], kv: KeyValue): List[MVIUnit] = {
        deserializeWithVin(vin=vin, rowkey=kv.key, value=kv.value).get :: acc
      }
    }
    folder(Nil, scanner)
  }


  /**
   * Прочитать всю CF из таблицы.
   * @return Будущий список [[MDVIActive]].
   */
  def getAllUnits(implicit ec:ExecutionContext): Future[List[MVIUnit]] = {
    val scanner = ahclient.newScanner(HTABLE_NAME)
    scanner.setFamily(CF)
    val folder = new AsyncHbaseScannerFold [List[MVIUnit]] {
      def fold(acc0: List[MVIUnit], kv: KeyValue): List[MVIUnit] = {
        deserializeRaw(rowkey=kv.key, qualifier=kv.qualifier, value=kv.value).get :: acc0
      }
    }
    folder(Nil, scanner)
  }

}

import MVIUnit._

trait MVIUnitStatic[T <: MVIUnit] extends CascadingFieldNamer {

  def ROW_KEY_PREFIX: String

  def isMyRowKey(rowkey: AnyRef): Boolean = {
    val rowkeyStr = SioModelUtil.deserialzeHCellCoord(rowkey)
    rowkeyStr startsWith ROW_KEY_PREFIX
  }

  def CF = MVIUnit.CF
  def HTABLE_NAME = MVIUnit.HTABLE_NAME
  def HTABLE_NAME_BYTES = MVIUnit.HTABLE_NAME_BYTES

  val VIN_FN        = fieldName("vin")
  val GENERATION_FN = fieldName("generation")

  /** Этот десериализатор вызывается из [[MVIUnit.deserializeRaw]] при наличии частично-десериализованных данных. */

  def deserializeSemiRaw(rowkey:Array[Byte], vin:String, value:Tuple, serVsn:Short): T

  /** Текущие тип+версия сериализации. Используется для дефолтового [[isSerVsnMatches]], выкинуть можно в любое время. */
  def SER_VSN: Short

  /** [[SER_VSN]] используется как для указания версии сохранённых данных, так и для принадлежности данных к тому
    * или иному типу. Для угадывания типа используется эта функция: MVIUnit опрашивает с помощью фунцкии
    * наследуемые модули на предмет попадания обнаруженного SER_VSN в их диапазон.
    * Если несколько версий, то функцию можно переопределить на более сложную логику. */
  def isSerVsnMatches(serVsn: Short): Boolean = SER_VSN == serVsn

  def isMyType(obj: MVIUnit): Boolean

  /** Внутренняя фунция для фильтрации юнитов по типу. */
  def filterUnitsByType(units: List[MVIUnit]): List[T] = {
    units.foldLeft (List[T]()) { (acc, e) =>
      if (isMyType(e))
        e.asInstanceOf[T] :: acc
      else
        acc
    }
  }

  /** Выдать все значения для указанного типа. */
  def getAllForVin(vin: String)(implicit ec:ExecutionContext): Future[List[T]] = {
    getAllUnitsForVin(vin) map {
      filterUnitsByType
    }
  }

  def getAll(implicit ec:ExecutionContext): Future[List[T]] = {
    getAllUnits map {
      filterUnitsByType
    }
  }
}

// Базовый интерфейс для классов, исповедующих доступ к dkey-индексам.
trait MVIUnit extends MacroLogsImpl {
  import LOGGER._

  def getTupleEntry: TupleEntry

  /** Указатель на объект-компаньон, чтобы получить доступ к статическим данным модели. */
  def companion: MVIUnitStatic[_]

  /** Выдать реальный ключ ряда в таблице хранилища. */
  @JsonIgnore
  def getRowKey: Array[Byte]

  /** Выдать ключ ряда в таблице в виде строки. */
  @JsonIgnore
  def getRowKeyStr: String = new String(getRowKey)

  @JsonIgnore
  def vin = getTupleEntry getString companion.VIN_FN
  protected def vin_=(vin: String) = {
    getTupleEntry.setString(companion.VIN_FN, vin)
    this
  }

  @JsonIgnore
  def generation = getTupleEntry getLong companion.GENERATION_FN
  protected def generation_=(generation: Long) = {
    getTupleEntry.setLong(companion.GENERATION_FN, generation)
    this
  }

  /** Эквивалент toString, выдающий максимально короткую строку. Используется для некоторых задач логгинга. */
  @JsonIgnore
  def toShortString: String

  /**
   * Выдать экземпляр модели MVirtualIndex. Линк между моделями по ключу.
   * Считаем, что вирт.индекс автоматически существует, если существует зависимый от него экземпляр сабжа.
   */
  @JsonIgnore
  def getVirtualIndex = MVirtualIndex(vin)

  /** В каких типа индекса производить поиск? */
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
  def deleteMappings(implicit client:Client, executor:ExecutionContext): Future[_]

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
        warn(s"$logPrefix vin=$vin used by others, other than '$getRowKeyStr'. Delete mapping...")
        deleteMappings

      case false => eraseBackingIndex
    }
  }


  /** Удалить весь индекс целиком, даже если в нём хранятся другие сайты (без проверок). */
  def eraseBackingIndex(implicit esClient: Client, ec:ExecutionContext): Future[Boolean] = {
    warn(s"eraseBackingIndex(): vin=$vin rowKey='$getRowKeyStr' :: Deleting real index FULLY!")
    getVirtualIndex.eraseShards
  }

  @JsonIgnore
  def getQualifier: Array[Byte] = serializeVin(vin)

  /** Сериализовать данные из кортежа в ключ-колонку-значение для HBase. */
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

  def isSameTypeAs(e: MVIUnit): Boolean = getClass == e.getClass

  override def toString: String = s"${getClass.getSimpleName}(rk=$getRowKeyStr, $vin, $generation)"
}


/**
 * Контейнер для набора, пригодного для немедленного сохранения в HBase.
 * @param rowkey Ключ ряда.
 * @param qualifier Имя колонки (qualifier).
 * @param value Байты payload'а.
 */
case class MVIUnitHBaseSerialized(rowkey:Array[Byte], qualifier:Array[Byte], value:Array[Byte])

