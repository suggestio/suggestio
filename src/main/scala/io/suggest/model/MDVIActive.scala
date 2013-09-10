package io.suggest.model

import org.apache.hadoop.fs.Path
import org.joda.time.LocalDate
import io.suggest.util.{JacksonWrapper, LogsImpl, SioFutureUtil}
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future, future}
import org.elasticsearch.client.{Client => EsClient}
import io.suggest.index_info.{MDVIUnitAlterable, MDVIUnit}
import org.elasticsearch.common.unit.TimeValue
import io.suggest.util.SiobixFs.fs
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.client.Get
import SioHBaseSyncClient.clientForTable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 */

object MDVIActive {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  val HTABLE_NAME = "mdviActive"

  /** Имя CF и колонки для метаданных активных индексов. */
  val CF_INX_ACTIVE = "a".getBytes
  val C_ACTIVE = CF_INX_ACTIVE

  val activeSubdirNamePath = new Path("active")

  private def hclient(implicit executor: ExecutionContext) = clientForTable(HTABLE_NAME)

  /**
   * Выдать путь до поддиректории /active, хранящей все файлы с данными об активных индексах.
   * @param dkey ключ домена.
   * @return Path для /active-поддиректории.
   */
  def getFsDkeyDirPath(dkey:String) = new Path(MDVIUnit.getDkeyPath(dkey), activeSubdirNamePath)

  /**
   * Сгенерить путь для dkey куда сохраняются данные этой модели.
   * @param dkey ключ домена.
   * @return путь.
   */
  def getFsDkeyPath(dkey:String, filename:String) = new Path(getFsDkeyDirPath(dkey), filename)

  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param filename Название, под которым сохранены данные. Обычно "default"
   * @return Опциональный сабж.
   */
  def getForDkeyNameFromFs(dkey:String, filename:String): Option[MDVIActive] = {
    getForFsPath(getFsDkeyPath(dkey, filename))
  }

  private def getForFsPath(p:Path): Option[MDVIActive] = {
    JsonDfsBackend.getAs[MDVIActive](p, fs)
  }

  private val globPath = getFsDkeyDirPath("*")

  /**
   * Найти все dkey, которые используют указанный индекс. Для этого нужно запросить
   * листинг /conf/dkey/.../active/[vin].
   * @param vin имя индекса, для которого проводится поиск.
   * @return Список dkey без повторяющихся элементов в произвольном порядке.
   */
  def findFsDkeysForIndex(vin:String)(implicit executor:ExecutionContext): Future[Seq[String]] = {
    debug("findDkeysForIndex(%s) starting" format vin)
    val glogPathVin = new Path(globPath, vin)
    future {
      fs.globStatus(glogPathVin) map { fstatus =>
        // Отмаппить в последовательность dkey, которые надо извлечь из path.
        val activePath = fstatus.getPath.getParent
        val dkeyIndexesPath = activePath.getParent
        val dkey = dkeyIndexesPath.getParent.getName
        dkey
      }
    }
  }


  /**
   * Асинхронно прочитать все активные индексы для указанного dkey.
   * @param dkey Ключ домена.
   * @return Будущий список MDVIActive.
   */
  def getFsAllForDkey(dkey:String)(implicit executor:ExecutionContext): Future[List[MDVIActive]] = {
    val path = getFsDkeyDirPath(dkey)
    future {
      fs.listStatus(path).foldLeft[List[MDVIActive]] (Nil) { (acc, s) =>
        if (s.isDir) {
          acc
        } else {
          val spath = s.getPath
          getForFsPath(spath) match {
            case Some(mdviA) => mdviA :: acc
            case None        => acc
          }
        }
      }
    }
  }

  /** Десериализовать набор байт в MDVIActive.
   * @param b Массив байтов.
   * @return Экземпляр MDVIActive.
   */
  def deserializeBytes(b: Array[Byte]): MDVIActive = {
    val is = new ByteArrayInputStream(b)
    try {
      JacksonWrapper.deserialize(is)
    } finally {
      is.close()
    }
  }

  /** Десереализовать из разных поддерживаемых форматов сериализации. Полезно при работе с данными из TupleEntry.
   */
  val desealizeAny: PartialFunction[AnyRef, MDVIActive] = {
    case s:   String                  => JacksonWrapper.deserialize(s)
    case ibw: ImmutableBytesWritable  => deserializeBytes(ibw.get)
    case b:   Array[Byte]             => deserializeBytes(b)
  }

  /** Сгенерить ключ исходя из ключа домена и vin.
   * @param dkey Ключ домена.
   * @param vin Строка vin, описывающая виртуальный индекс.
   * @return Строку, которая используется в качестве ключа.
   */
  def dkeyVin2rowKey(dkey:String, vin:String) = dkey + "/" + vin

  /** Прочитать значение сабжа из таблицы по ключу.
   * @param dkey Ключ домена.
   * @param vin Строка vin, хранящий данные об используемом индексе.
   * @return Экземпляр MDVIActive, если найден.
   */
  def getForDkeyVinHbase(dkey: String, vin:String)(implicit executor: ExecutionContext): Future[Option[MDVIActive]] = {
    // TODO Задействовать асинхронный клиент при появлении возможности, ибо тут адски блокирующаяся поделка.
    val getReq = new Get(dkeyVin2rowKey(dkey, vin).getBytes)
      .addColumn(CF_INX_ACTIVE, C_ACTIVE)
    hclient.map { _client =>
      try {
        val results = _client.get(getReq)
        if (results.isEmpty) {
          None
        } else {
          val bytes = results.getColumnLatest(CF_INX_ACTIVE, C_ACTIVE).getValue
          Some(deserializeBytes(bytes))
        }
      } finally {
        _client.close()
      }
    }
  }

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

  /**
   * Выдать имя файла, под которым будут сохраняться данные этого экземпляра.
   * @return Строка имени файла, без пути, файлового расширения и т.д.
   */
  @JsonIgnore
  def filename: String = vin

  /**
   * Путь к файлу в DFS, пригодный для отправки в JsonDfsBackend.
   * @return Путь.
   */
  @JsonIgnore
  def filepath: Path = getFsDkeyPath(dkey, filename)


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
  def getTypesForRequest(sc:SioSearchContext): List[String] = {
    error("getTypesForRequest: NOT YET IMPLEMENTED")
    getAllTypes  // TODO нужно как-то что-то где-то определять.
  }

  /**
   * Сохранить текущий экземпляр в хранилище.
   * @return Сохраненные (т.е. текущий) экземпляр сабжа.
   */
  @JsonIgnore
  def save: MDVIActive = {
    JsonDfsBackend.writeToPath(path=filepath, value=this, overwrite=false)
    this
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
  def setMappings(failOnError:Boolean = true)(implicit esClient:EsClient, executor:ExecutionContext): Future[Boolean] = {
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
  def deleteMappings(implicit esClient:EsClient, executor:ExecutionContext): Future[Unit] = {
    debug("deleteMappings(): dkey=%s vin=%s types=%s" format (dkey, vin, subshards map { _.getTypename }))
    SioFutureUtil
      .mapLeftSequentally(subshards, ignoreErrors=true) { _.deleteMappaings() }
      .map(_ => Unit)
  }

  /**
   * Используется ли указанный vin другими доменами?
   * @return true, если используется.
   */
  def isVinUsedByOtherDkeys(implicit executor:ExecutionContext): Future[Boolean] = {
    findFsDkeysForIndex(vin) map { vinUsedBy =>
      var l = vinUsedBy.length
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
  def deleteIndexOrMappings(implicit esClient: EsClient, executor: ExecutionContext): Future[Unit] = {
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

