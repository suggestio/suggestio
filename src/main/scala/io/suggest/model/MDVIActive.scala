package io.suggest.model

import org.joda.time.LocalDate
import io.suggest.util._
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.{Client => EsClient}
import io.suggest.index_info.MDVIUnitAlterable
import org.elasticsearch.common.unit.TimeValue
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.hadoop.hbase.HColumnDescriptor
import scala.collection.JavaConversions._
import HTapConversionsBasic._
import SioHBaseAsyncClient._
import org.hbase.async.{DeleteRequest, PutRequest, KeyValue, GetRequest}
import cascading.tuple.{TupleEntry, Fields, Tuple}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import scala.Some
import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 *
 * В этой модели исторически жили два вида сериализации: кусочная-долгосрочная в json и полноразмерная-промежуточная в dkeyless Tuple (использовалось в flow).
 * 2013.dec.20 был рассадник сериализаций был заменён новым, менее разнообразным.
 * Долгосрочное хранение происходит через tuple с номером версии сериализатора в первом поле, и без dkey и vin - они в rowKey и qualifier.
 * При этом экземпляр MDVIActive является легко сериализуемым, ибо является подклассом BaseDatum, что позволяет гибко
 * пользоваться им внутри flow.
 *
 * Сеттеры модели не публичные, т.к. экземплярым модели изначально разработа чтобы быть неизменяемой.
 */
object MDVIActive extends CascadingFieldNamer {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Короткие подсказки для модели и её внешних пользователей, которым не важно внутреннее устройство модели.
  def HTABLE_NAME = MObject.HTABLE_NAME
  def HTABLE_NAME_BYTES = MObject.HTABLE_NAME_BYTES

  def CF = MObject.CF_DINX_ACTIVE

  val DKEY_FN       = fieldName("dkey")
  val VIN_FN        = fieldName("vin")
  val GENERATION_FN = fieldName("generation")
  val SUBSHARDS_FN  = fieldName("subshards")
  val FIELDS        = new Fields(DKEY_FN, VIN_FN, GENERATION_FN, SUBSHARDS_FN)


  // Номер поколения - это миллисекунды, вычтенные и деленные на указанный делитель.
  // Номер поколения точно не будет раньше этого времени. Выкидываем.
  // После запуска в продакшен, менять эти цифры уже нельзя (понадобится полное удаление всех данных по индексам вместе с оными).
  val GENERATION_SUBSTRACT  = 1381140627123L // 2013/oct/07 14:11 +04:00.
  val GENERATION_RESOLUTION = 10000L         // Округлять мс до 10 секунд.

  /** Это поколение выставляется для сабжа, если  */
  val GENERATION_BOOTSTRAP = -1

  /** Версия, указываемая нулевым полем в сериализуемые кортежи. 16-бит хватит для указания любой возможной версии.
    * Это необходимо для возможности легкого обновления схемы данных. Версию проверяет десериализатор в deserializeWithDkeyVin(). */
  val SER_VSN = 1.toShort


  /** Десериализовать hbase qualifier в строку vin. */
  val deserializeQualifier2Vin: PartialFunction[AnyRef, String] = {
    case bytes: Array[Byte] =>
      val dkeyRev: String = new String(bytes)
      deserializeQualifier2Vin(dkeyRev)

    case str: String => str

    case ibw: ImmutableBytesWritable =>
      deserializeQualifier2Vin(ibw.get)
  }


  /** Десериализовать hbase rowkey в dkey-строку. */
  def deserializeRowkey2Dkey(rowkey: AnyRef) = {
    val dkeyRev = deserializeQualifier2Vin(rowkey)
    UrlUtil.reverseDomain(dkeyRev)
  }


  /** Сериализовать dkey в ключ ряда. Для удобства сортировки поддоменов, dkey разворачивается в "ru.domain.subdomain".
    * @param dkey Ключ домена.
    * @return Байты, пригодные для задания ключа в таблице.
    */
  def serializeDkey2Rowkey(dkey: String): Array[Byte] = UrlUtil.reverseDomain(dkey).getBytes

  /** Сериализовать vin. По сути просто конвертится строка в байты.
   * @param vin Строка виртуального индекса.
   * @return Байты, пригодные для задание qualifier'a.
   */
  def serializeVin(vin: String): Array[Byte] = vin.getBytes


  /** Десериализовать исходную запись из HBase.
    * @param rowkey Ключ ряда.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return MDVIActive.
    */
  def deserializeRaw(rowkey:AnyRef, qualifier:AnyRef, value:Array[Byte]): MDVIActive = {
    val dkey = deserializeRowkey2Dkey(rowkey)
    deserializeWithDkey(dkey, qualifier, value=value)
  }

  /** Десериализовать данные после хранения в HBase. На случай, если dkey уже десериализован.
    * @param dkey Ключ домена.
    * @param qualifier Имя колонки (vin).
    * @param value Значение ячейки.
    * @return MDVIActive.
    */
  def deserializeWithDkey(dkey:String, qualifier:AnyRef, value:Array[Byte]): MDVIActive = {
    val vin = deserializeQualifier2Vin(qualifier)
    deserializeWithDkeyVin(dkey=dkey, vin=vin, value=value)
  }

  /** Десериализовать данные при условии, что vin уже десериализован.
   * @param rowkey Ключ ряда.
   * @param vin Строка виртуального индекса.
   * @param value Значение ячейки.
   * @return MDVIActive
   */
  def deserializeWithVin(rowkey:AnyRef, vin:String, value:Array[Byte]): MDVIActive = {
    val dkey = deserializeRowkey2Dkey(rowkey)
    deserializeWithDkeyVin(dkey=dkey, vin=vin, value=value)
  }


  /** Десериализации при наличии готовых vin и dkey.
    * @param dkey Ключ домена.
    * @param vin Строка виртуального индекса.
    * @param value Значение ячейки.
    * @return MDVIActive.
    */
  def deserializeWithDkeyVin(dkey:String, vin:String, value:Array[Byte]): MDVIActive = {
    val tuple = SerialUtil.deserializeTuple(value)
    val vsn = tuple getShort 0
    vsn match {
      case SER_VSN =>
        val generation = tuple getLong 1
        val sisSer = tuple getObject 2
        new MDVIActive(dkey=dkey, vin=vin, generation=generation, subshardsRaw=sisSer)
    }
  }


  /** Сериализовать запись в кортеж и реквизиты. Результат пригоден для немедленной отправки в HBase.
    * @param mdvia Исходный MDVIActive.
    * @return Контейнер, содержащий готовые данные для отправки в HBase.
    */
  def serializeForHBase(mdvia: MDVIActive): MDVIActiveSerialized = {
    val rowkey = serializeDkey2Rowkey(mdvia.getDkey)
    val qualifier = deserializeQualifier2Vin(mdvia.getVin)
    val value = new Tuple
    value addShort SER_VSN
    value addLong mdvia.getGeneration
    value add mdvia.getSubshardsRaw
    val ssisTuple = new Tuple
    value add ssisTuple
    val valBytes = SerialUtil.serializeTuple(value)
    MDVIActiveSerialized(rowkey=rowkey, qualifier=qualifier, value=valBytes)
  }


  val deserializeSubshardInfo: PartialFunction[AnyRef, List[MDVISubshardInfo]] = {
    case t: Tuple =>
      t.toList.map { MDVISubshardInfo.deserializeV1(_) }
  }


  def serializeSubshardInfo(sis: List[MDVISubshardInfo]): Tuple = {
    val ssisTuple = new Tuple
    sis.map(_.serializerToTuple)
       .foreach(ssisTuple add _)
    ssisTuple
  }


  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param vin vin, определяющий шардинг виртуального индекса.
   * @return Опциональный сабж.
   */
  def getForDkeyVin(dkey:String, vin:String)(implicit ec:ExecutionContext): Future[Option[MDVIActive]] = {
    val column: Array[Byte] = vin
    val rowkey = serializeDkey2Rowkey(dkey)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF)
      .qualifier(column)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val bytes = results.head.value()
        val mdvia = deserializeWithDkeyVin(dkey=dkey, vin=vin, value=bytes)
        Some(mdvia)
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
    val folder = new AsyncHbaseScannerFold[List[MDVIActive]] {
      def fold(acc: List[MDVIActive], kv: KeyValue): List[MDVIActive] = {
        deserializeWithVin(vin=vin, rowkey=kv.key, value=kv.value) :: acc
      }
    }
    folder(Nil, scanner)
  }


  /**
   * Прочитать всю CF из таблицы.
   * @return Будущий список MDVIActive.
   */
  def getAll(implicit ec:ExecutionContext): Future[List[MDVIActive]] = {
    val scanner = ahclient.newScanner(HTABLE_NAME)
    scanner.setFamily(CF)
    val folder = new AsyncHbaseScannerFold [List[MDVIActive]] {
      def fold(acc0: List[MDVIActive], kv: KeyValue): List[MDVIActive] = {
        deserializeRaw(rowkey=kv.key, qualifier=kv.qualifier, value=kv.value) :: acc0
      }
    }
    folder(Nil, scanner)
  }


  /** Асинхронно прочитать все активные индексы для указанного dkey. Быстрая операция.
   * @param dkey Ключ домена.
   * @return Будущий список MDVIActive.
   */
  def getAllForDkey(dkey: String)(implicit ec:ExecutionContext): Future[Seq[MDVIActive]] = {
    val rowkey = serializeDkey2Rowkey(dkey)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        Nil
      } else {
        results.map { kv =>
          deserializeWithDkey(dkey=dkey, qualifier=kv.qualifier, value=kv.value)
        }
      }
    }
  }


  /** Выдать CF-дескриптор для используемого CF
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF).setMaxVersions(3)


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
 */
class MDVIActive extends BaseDatum(FIELDS) with MDVIUnitAlterable {

  import LOGGER._

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  /** Конструктор.
   * @param dkey Ключ домена
   * @param vin Имя нижележащего виртуального индекса.
   * @param generation Поколение. При миграции в другой индекс поколение увеличивается.
   * @param subshardsInfo Список подшард. "Новые" всегла левее (ближе к head), чем старые. В последнем хвосте списка
   *                      всегда находится шарда с lowerDaysCount = 0.
   */
  def this(
    dkey          : String,
    vin           : String,
    generation    : Long,
    subshardsInfo : List[MDVISubshardInfo] = MDVIActive.SUBSHARD_INFO_DFLT
  ) = {
    this()
    if (subshardsInfo.isEmpty) {
      throw new IllegalArgumentException("subshardInfo MUST contain at least one shard. See default value for example.")
    }
    setDkey(dkey)
    setVin(vin)
    setGeneration(generation)
    setSubshardsInfo(subshardsInfo)
  }

  private def this(dkey:String, vin:String, generation:Long, subshardsRaw:AnyRef) {
    this()
    setDkey(dkey)
    setVin(vin)
    setGeneration(generation)
    setSubshardsRaw(subshardsRaw)
  }


  def getDkey = _tupleEntry getString DKEY_FN
  protected def setDkey(dkey: String) = {
    _tupleEntry.setString(DKEY_FN, dkey)
    this
  }

  def getVin = _tupleEntry getString VIN_FN
  protected def setVin(vin: String) = {
    _tupleEntry.setString(VIN_FN, vin)
    this
  }

  def getGeneration = _tupleEntry getLong GENERATION_FN
  protected def setGeneration(generation: Long) = {
    _tupleEntry.setLong(GENERATION_FN, generation)
    this
  }

  def getSubshardsRaw = {
    (_tupleEntry getObject SUBSHARDS_FN).asInstanceOf[Tuple]
  }
  def getSubshardsInfo: List[MDVISubshardInfo] = {
    val sisSer = getSubshardsRaw
    deserializeSubshardInfo(sisSer)
  }

  protected def setSubshardsRaw(sisSer: AnyRef) = {
    _tupleEntry.setObject(SUBSHARDS_FN, sisSer)
    this
  }
  protected def setSubshardsInfo(sis: List[MDVISubshardInfo]) = {
    val sisSer = serializeSubshardInfo(sis)
    setSubshardsRaw(sisSer)
  }


  protected implicit def toSubshard(sInfo: MDVISubshardInfo) = new MDVISubshard(this, sInfo)
  protected implicit def toSubshardsList(sInfoList: List[MDVISubshardInfo]) = sInfoList.map(toSubshard)

  /**
   * Отразить список подшард в виде полноценных объектов Subshard, ссылкающихся на своего родителя.
   * @return Список MDVISubshard.
   */
  @JsonIgnore
  def getSubshards: List[MDVISubshard] = getSubshardsInfo

  /**
   * Выдать id этого виртуального индекса.
   * @return
   */
  @JsonIgnore
  def id: String = "%s$%s$%s" format (getDkey, getVin, getGeneration)

  /**
   * Нужно сохранить документ. И встает вопрос: в какую именно подшарду.
   * @param d дата документа.
   * @return подшарду для указанной даты. Из неё можно получить название типа при необходимости.
   */
  def getSubshardForDate(d: LocalDate): MDVISubshard = {
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
      case true  => getSubshards.head

      // TODO Выбираются все шарды слева направо, однако это может неэффективно.
      //      Следует отбрасывать "свежие" шарды слева, если их многовато.
      case false =>
        val sis = getSubshardsInfo
        val si = sis find { _.lowerDateDays < daysCount } getOrElse sis.last
        si
    }
  }

  /**
   * Когда настало время сохранять что-то, то нужно выбрать шарду подходящую.
   * @param d дата документа
   * @return кортеж индекса и типа.
   */
  def getInxTypeForDate(d: LocalDate): (String, String) = {
    val days = toDaysCount(d)
    val ss = getSubshardForDaysCount(days)
    val inx = ss.getShardForDaysCount(days)
    inx -> ss.getTypename
  }

  @JsonIgnore
  lazy val isSingleShard = getSubshards.length == 1

  /**
   * Выдать все типы, относящиеся ко всем индексам в этой подшарде.
   * @return список типов.
   */
  @JsonIgnore
  def getAllTypes = getSubshards.map(_.getTypename)


  /**
   * Выдать экземпляр модели MVirtualIndex. Линк между моделями по ключу.
   * Считаем, что вирт.индекс точно существует, если существует зависимый от него экземпляр сабжа.
   */
  @JsonIgnore
  lazy val getVirtualIndex = MVirtualIndex(getVin)


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


  /**
   * Сохранить текущий экземпляр в хранилище.
   * @return Сохраненные (т.е. текущий) экземпляр сабжа.
   */
  @JsonIgnore
  def save(implicit ec: ExecutionContext): Future[_] = {
    val ser = MDVIActive.serializeForHBase(this)
    val putReq = new PutRequest(HTABLE_NAME_BYTES, ser.rowkey, CF.getBytes, ser.qualifier, ser.value)
    ahclient.put(putReq)
  }

  /**
   * Удалить ряд из хранилища. Проверку по generation не делаем, ибо оно неразрывно связано с dkey+vin и является
   * вспомогательной в рамках flow величиной.
   * @return Фьючерс для синхронизации.
   */
  def delete(implicit ec: ExecutionContext): Future[Unit] = {
    val delReq = new DeleteRequest(HTABLE_NAME_BYTES, getDkey:Array[Byte], CF.getBytes, getVin:Array[Byte])
    ahclient.delete(delReq) map { _ => () }
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
    val subshards = getSubshards
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
        val msg = s"Failed to setMappings on one or more subshards. $subshards => $boolSeq"
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
    val subshards = getSubshards
    debug(s"deleteMappings(): dkey=$getDkey vin=$getVin types=${ subshards.map(_.getTypename)}")
    SioFutureUtil
      .mapLeftSequentally(subshards, ignoreErrors=true) { _.deleteMappaings() }
      .map(_ => Unit)
  }

  /**
   * Используется ли указанный vin другими доменами?
   * @return true, если используется.
   */
  def isVinUsedByOtherDkeys(implicit ec:ExecutionContext): Future[Boolean] = {
    getAllLatestForVin(getVin) map { vinUsedBy =>
      var l = vinUsedBy.size
      if (vinUsedBy contains getDkey)
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
  def eraseIndexOrMappings(implicit esClient: EsClient, ec: ExecutionContext): Future[_] = {
    val logPrefix = "deleteIndexOrMappings():"
    isVinUsedByOtherDkeys flatMap {
      case true  =>
        warn(s"$logPrefix vin=$getVin used by dkeys, other than '$getDkey'. Delete mapping...")
        deleteMappings

      case false => eraseBackingIndex
    }
  }


  /** Удалить весь индекс целиком, даже если в нём хранятся другие сайты (без проверок). */
  def eraseBackingIndex(implicit esClient: EsClient, ec:ExecutionContext): Future[Boolean] = {
    warn(s"eraseBackingIndex(): vin=$getVin dkey='$getDkey' Deleting real index FULLY!")
    getVirtualIndex.eraseShards
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


/**
 * Контейнер для результата serializeForHBase().
 * @param rowkey Ключ ряда.
 * @param qualifier Имя колонки (qualifier).
 * @param value Байты payload'а.
 */
case class MDVIActiveSerialized(rowkey:Array[Byte], qualifier:Array[Byte], value:Array[Byte])

