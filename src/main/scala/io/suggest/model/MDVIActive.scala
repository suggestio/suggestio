package io.suggest.model

import org.apache.hadoop.fs.Path
import org.joda.time.LocalDate
import io.suggest.util.{LogsPrefixed, SioFutureUtil, Logs}
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future, future}
import org.elasticsearch.client.Client
import io.suggest.index_info.{MDVIUnitAlterable, MDVIUnit}
import org.elasticsearch.common.unit.TimeValue
import io.suggest.util.SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 */

object MDVIActive extends Logs {

  val activeSubdirNamePath = new Path("active")

  /**
   * Выдать путь до поддиректории /active, хранящей все файлы с данными об активных индексах.
   * @param dkey ключ домена.
   * @return Path для /active-поддиректории.
   */
  def getDkeyActiveDirPath(dkey:String) = new Path(MDVIUnit.getDkeyPath(dkey), activeSubdirNamePath)

  /**
   * Сгенерить путь для dkey куда сохраняются данные этой модели.
   * @param dkey ключ домена.
   * @return путь.
   */
  def getDkeyPath(dkey:String, filename:String) = new Path(getDkeyActiveDirPath(dkey), filename)

  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param fileame Название, под которым сохранены данные. Обычно "default"
   * @return Опциональный сабж.
   */
  def getForDkeyName(dkey:String, fileame:String): Option[MDVIActive] = {
    JsonDfsBackend.getAs[MDVIActive](getDkeyPath(dkey, fileame), fs)
  }

  private val glogPath = getDkeyActiveDirPath("*")

  /**
   * Найти все dkey, которые используют указанный индекс. Для этого нужно запросить листинг
   * /conf/dkey/.../active/$vin
   * @param vin имя индекса, для которого проводится поиск.
   * @return Список dkey без повторяющихся элементов в произвольном порядке.
   */
  def findDkeysForIndex(vin:String)(implicit executor:ExecutionContext): Future[Seq[String]] = {
    debug("findDkeysForIndex(%s) starting" format vin)
    val glogPathVin = new Path(glogPath, vin)
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

}


import MDVIActive._

/**
 * Класс уровня домена. Хранит информацию по виртуальному индексу в контексте текущего dkey.
 * @param dkey Ключ домена
 * @param vin Имя нижележащего виртуального индекса.
 * @param subshards Список подшард.
 * @param generation Поколение. При миграции в другой индекс поколение инкрементируется.
 */
case class MDVIActive(
  dkey:       String,
  vin:        String,
  generation: Int = 0,
  subshards:  List[MDVISubshard] = List(new MDVISubshard(this, 0))

) extends MDVIUnitAlterable with LogsPrefixed {

  def id: String = "%s$%s$%s" format (dkey, vin, generation)

  protected val logPrefix: String = "%s/%s" format (dkey, vin)

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
      case false => subshards find { _.lowerDateDays < daysCount } getOrElse subshards.last
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
  def filename: String = vin

  /**
   * Путь к файлу в DFS, пригодный для отправки в JsonDfsBackend.
   * @return Путь.
   */
  def filepath: Path = getDkeyPath(dkey, filename)


  lazy val isSingleShard = subshards.length == 1

  /**
   * Выдать все типы, относящиеся ко всем индексам в этой подшарде.
   * @return список типов.
   */
  def getAllTypes = subshards.map(_.getTypename)

  /**
   * Выдать все типы, лежащие на указанной шарде.
   * @param shardN номер родительской шарды
   * @return списочек подшард
   */
  def getAllTypesForShard(shardN: Int): List[MDVISubshard] = {
    subshards.foldLeft[List[MDVISubshard]] (Nil) { (acc, ss) =>
      if (ss.shards contains shardN) {
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
  def getVirtualIndex: MVirtualIndex = MVirtualIndex.getForVin(vin).get


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
   * @param fs Используемая DFS
   * @return Сохраненные (т.е. текущий) экземпляр сабжа.
   */
  def save: MDVIActive = {
    JsonDfsBackend.writeToPath(path=filepath, value=this, overwrite=false)
    this
  }

  /**
   * Выдать натуральные шарды натурального индекса, обратившись к вирт.индексу.
   * @return Список названий индексов.
   */
  def getShards = getVirtualIndex.getShards

  /**
   * Выставить маппинги для всех подшард.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return true, если всё хорошо.
   */
  def setMappings(failOnError:Boolean = true)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
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
  def deleteMappings(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
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
    findDkeysForIndex(vin) map { vinUsedBy =>
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
  def deleteIndexOrMappings(implicit client: Client, executor: ExecutionContext): Future[Unit] = {
    isVinUsedByOtherDkeys map {
      case true  => deleteMappings
      case false => getVirtualIndex.delete
    }
  }


  import io.suggest.util.SioEsIndexUtil._

  /**
   * Запуск полного скроллинга по индексу.
   * @param timeout Время жизни курсора на стороне ES.
   * @param sizePerShard Сколько брать из шарды за один шаг.
   * @return Фьючес ответа, содержащего скроллер и прочую инфу.
   */
  def startFullScroll(timeout:TimeValue = SCROLL_TIMEOUT_INIT_DFLT, sizePerShard:Int = SCROLL_PER_SHARD_DFLT)(implicit client:Client) = {
    startFullScrollIn(getShards, getAllTypes, timeout, sizePerShard)
  }

}

