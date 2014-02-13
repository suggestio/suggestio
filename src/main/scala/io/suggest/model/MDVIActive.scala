package io.suggest.model

import org.joda.time.LocalDate
import io.suggest.util._
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.{Client => EsClient}
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import HTapConversionsBasic._
import SioHBaseAsyncClient._
import org.hbase.async.{KeyValue, GetRequest}
import cascading.tuple.{TupleEntry, Fields, Tuple}
import com.scaleunlimited.cascading.BaseDatum
import io.suggest.model.MVIUnit._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 *
 * Долгосрочное хранение экземпляра происходит через tuple с номером версии сериализатора в первом поле,
 * и без dkey и vin - они в rowKey и qualifier.
 * При этом экземпляр MDVIActive является сам по себе легко сериализуемым напрямую, ибо является подклассом BaseDatum,
 * что позволяет прозрачно передавать MDVIActive внутри flow.
 *
 * Сеттеры модели не публичные: модель должна быть (или хотя бы выглядеть) неизменяемой. Она там спроектирована.
 */
object MDVIActive extends MVIUnitStatic with MacroLogsImpl {
  import LOGGER._

  val DKEY_FN       = fieldName("dkey")
  val SUBSHARDS_FN  = fieldName("subshards")
  val FIELDS        = new Fields(DKEY_FN, VIN_FN, GENERATION_FN, SUBSHARDS_FN)


  /** Версия, указываемая нулевым полем в сериализуемые кортежи. 16-бит хватит для указания любой возможной версии.
    * Это необходимо для возможности легкого обновления схемы данных. Версию проверяет десериализатор в [[deserializeWithDkeyVin]]. */
  val SER_VSN = 1.toShort


  /** Десериализовать hbase rowkey в dkey-строку. */
  def deserializeRowkey2Dkey(rowkey: AnyRef) = SioModelUtil.rowkey2dkey(rowkey)

  /** Сериализовать dkey в ключ ряда. Для удобства сортировки поддоменов, dkey разворачивается в "ru.domain.subdomain".
    * @param dkey Ключ домена.
    * @return Байты, пригодные для задания ключа в таблице.
    */
  def serializeDkey2Rowkey(dkey: String): Array[Byte] = SioModelUtil.dkey2rowkey(dkey)


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
    * @return [[MDVIActive]].
    */
  def deserializeWithDkey(dkey:String, qualifier:AnyRef, value:Array[Byte]): MDVIActive = {
    val vin = deserializeQualifier2Vin(qualifier)
    deserializeWithDkeyVin(dkey=dkey, vin=vin, value=value)
  }

  /** Десериализовать данные при условии, что vin уже десериализован.
   * @param rowkey Ключ ряда.
   * @param vin Строка виртуального индекса.
   * @param value Значение ячейки.
   * @return [[MDVIActive]].
   */
  def deserializeWithVin(rowkey:AnyRef, vin:String, value:Array[Byte]): MDVIActive = {
    val dkey = deserializeRowkey2Dkey(rowkey)
    deserializeWithDkeyVin(dkey=dkey, vin=vin, value=value)
  }


  /** Десериализации при наличии готовых vin и dkey.
    * @param dkey Ключ домена.
    * @param vin Строка виртуального индекса.
    * @param value Значение ячейки.
    * @return [[MDVIActive]].
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
    * @param mdvia Исходный [[MDVIActive]].
    * @return Контейнер, содержащий готовые данные для отправки в HBase.
    */
  def serializeForHBase(mdvia: MDVIActive): MVIUnitHBaseSerialized = {
    val rowkey = serializeDkey2Rowkey(mdvia.dkey)
    val qualifier = deserializeQualifier2Vin(mdvia.vin)
    val value = new Tuple
    value addShort SER_VSN
    value addLong mdvia.generation
    value add mdvia.subshardsRaw
    val ssisTuple = new Tuple
    value add ssisTuple
    val valBytes = SerialUtil.serializeTuple(value)
    MVIUnitHBaseSerialized(rowkey=rowkey, qualifier=qualifier, value=valBytes)
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
    val rowkey = serializeDkey2Rowkey(dkey)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF)
      .qualifier(serializeVin(vin))
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
   * @return Список (dkey, [[MDVIActive]]) без повторяющихся элементов в произвольном порядке.
   *         При желании можно сконвертить в карту через .toMap()
   */
  def getAllLatestForVin(vin: String)(implicit ec:ExecutionContext): Future[List[MDVIActive]] = {
    trace(s"getAllForVin($vin): Starting...")
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
   * @return Будущий список [[MDVIActive]].
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
   * @return Будущий список [[MDVIActive]].
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


  /** Дефолтовое значение поля subshardsInfo. */
  def SUBSHARD_INFO_DFLT = List(new MDVISubshardInfo(0))

}


import MDVIActive._

/** Класс уровня домена. Хранит информацию по виртуальному индексу в контексте текущего dkey. */
class MDVIActive extends BaseDatum(FIELDS) with MVIUnit {

  import this.LOGGER._

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  /** Вспомогательный конструктор, дедублицирующий функции между другими конструкторами. */
  protected def this(dkey: String, vin: String, generation: Long) = {
    this()
    this.dkey = dkey
    this.vin = vin
    this.generation = generation
  }

  /** Основной конструктор датума на основе данных полей.
   * @param dkey Ключ домена. Поиск в хранилище происходит именно по этому dkey.
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
    this(dkey, vin, generation)
    if (subshardsInfo.isEmpty) {
      throw new IllegalArgumentException("subshardInfo MUST contain at least one shard. See default value for example.")
    } else {
      subshardsInfo_=(subshardsInfo)
    }
  }

  private def this(dkey:String, vin:String, generation:Long, subshardsRaw:AnyRef) {
    this(dkey, vin, generation)
    subshardsRaw_=(subshardsRaw)
  }


  /** Выдать реальный ключ ряда в таблице хранилища. */
  override def getRowKey: Array[Byte] = serializeDkey2Rowkey(dkey)

  @JsonIgnore
  def dkey = _tupleEntry getString DKEY_FN
  protected def dkey_=(dkey: String) = {
    _tupleEntry.setString(DKEY_FN, dkey)
    this
  }

  def vin = _tupleEntry getString VIN_FN
  protected def vin_=(vin: String) = {
    _tupleEntry.setString(VIN_FN, vin)
    this
  }

  def generation = _tupleEntry getLong GENERATION_FN
  protected def generation_=(generation: Long) = {
    _tupleEntry.setLong(GENERATION_FN, generation)
    this
  }

  @JsonIgnore
  def subshardsRaw = (_tupleEntry getObject SUBSHARDS_FN).asInstanceOf[Tuple]

  protected def subshardsRaw_=(sisSer: AnyRef) = {
    _tupleEntry.setObject(SUBSHARDS_FN, sisSer)
    this
  }
  
  
  @JsonIgnore
  def subshardsInfo: List[MDVISubshardInfo] = {
    val sisSer = subshardsRaw
    deserializeSubshardInfo(sisSer)
  }

  protected def subshardsInfo_=(sis: List[MDVISubshardInfo]) = {
    val sisSer = serializeSubshardInfo(sis)
    subshardsRaw_=(sisSer)
  }


  protected implicit def toSubshard(sInfo: MDVISubshardInfo) = new MDVISubshard(this, sInfo)
  protected implicit def toSubshardsList(sInfoList: List[MDVISubshardInfo]) = sInfoList.map(toSubshard)

  /**
   * Отразить список подшард в виде полноценных объектов Subshard, ссылкающихся на своего родителя.
   * @return Список [[MDVISubshard]].
   */
  @JsonIgnore
  def getSubshards: List[MDVISubshard] = subshardsInfo

  /**
   * Выдать id этого виртуального индекса.
   * @return Короткая идентификационная строка, пригодная для отправки в логи.
   */
  @JsonIgnore
  def toShortString: String = "%s/%s/%s" format (dkey, vin, generation)


  /**
   * Выдать шарду для указанного кол-ва дней.
   * @param daysCount кол-во дней.
   * @return название шарды.
   */
  def getSubshardForDaysCount(daysCount: Int): MDVISubshard = {
    isSingleShard match {
      case true  => getSubshards.head

      // TODO Выбираются все шарды слева направо, однако это может неэффективно.
      //      Следует отбрасывать "свежие" шарды слева, если их многовато.
      case false =>
        val sis = subshardsInfo
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

  def isSingleShard = getSubshards.length == 1

  /**
   * Выдать все типы, относящиеся ко всем индексам в этой подшарде.
   * @return список типов.
   */
  def getAllTypes = getSubshards.map(_.getTypename)


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


  def serializeForHbase: MVIUnitHBaseSerialized = MDVIActive.serializeForHBase(this)


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
    debug(s"deleteMappings(): dkey=$dkey vin=$vin types=${ subshards.map(_.getTypename)}")
    SioFutureUtil
      .mapLeftSequentally(subshards, ignoreErrors=true) { _.deleteMappaings() }
      .map(_ => Unit)
  }


  /**
   * Используется ли указанный vin другими доменами?
   * @return true, если используется.
   */
  def isVinUsedByOthers(implicit esClient:EsClient, ec:ExecutionContext): Future[Boolean] = {
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


  @JsonIgnore
  override def toString: String = s"${getClass.getSimpleName}($dkey, $vin, $generation, ${subshardsRaw.size} subshards)"
}

