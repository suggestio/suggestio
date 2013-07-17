package io.suggest.model

import org.apache.hadoop.fs.{FileSystem, Path}
import org.joda.time.LocalDate
import io.suggest.util.{SioFutureUtil, Logs}
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.index_info.{MDVIUnitAlterable, MDVIUnit}
import org.elasticsearch.common.unit.TimeValue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 */

object MDVIActive {

  val activeSubdirNamePath = new Path("active")
  val activeDirRelPath     = new Path(MDVIUnit.rootDirNamePath, activeSubdirNamePath)

  /**
   * Сгенерить путь для dkey куда сохраняются данные этой модели.
   * @param dkey ключ домена.
   * @return путь.
   */
  def getDkeyPath(dkey:String, filename:String) = new Path(MDVIUnit.getDkeyPath(dkey), activeSubdirNamePath)

  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param fileame Название, под которым сохранены данные. Обычно "default"
   * @return Опциональный сабж.
   */
  def getForDkeyName(dkey:String, fileame:String)(implicit fs:FileSystem): Option[MDVIActive] = {
    JsonDfsBackend.getAs[MDVIActive](getDkeyPath(dkey, fileame), fs)
  }

}


import MDVIActive._

/**
 * Класс уровня домена. Хранит информацию по виртуальному индексу в контексте текущего dkey.
 * @param dkey Ключ домена
 * @param vin Имя нижележащего виртуального индекса.
 * @param filename В рамках dkey используется этот идентификатор.
 * @param subshards Список подшард.
 * @param generation Поколение. При миграции в другой индекс поколение инкрементируется.
 */
case class MDVIActive(
  dkey:       String,
  vin:        String,
  filename:   String = "default",
  generation: Int = 0,
  subshards:  List[MDVISubshard] = List(new MDVISubshard(this, 0))

) extends MDVIUnitAlterable with Logs {

  def id: String = "%s$%s$%s" format (dkey, vin, generation)

  /**
   * Нужно сохранить документ. И встает вопрос: в какую именно подшарду.
   * @param d дата документа.
   * @return подшарду для указанной даты. Из неё можно получить название типа при необходимости.
   */
  def getSubshardForDate(d:LocalDate): MDVISubshard = {
    val days = toDaysCount(d)
    // Ищем шарду, удовлетворяющую дате
    isSingleShard match {
      case true  => subshards.head

      // TODO Выбираются все шарды слева направо, однако это может неэффективно.
      //      Следует отбрасывать "свежие" шарды слева, если их многовато.
      case false => subshards find { _.lowerDateDays < days } getOrElse subshards.last
    }
  }

  lazy val isSingleShard = subshards.length == 1

  /**
   * Выдать все типы, относящиеся ко всем индексам в этой подшарде.
   * @return список типов.
   */
  def getAllTypes = subshards.map(_.typename)

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
  def save(implicit fs:FileSystem): MDVIActive = {
    val path = getDkeyPath(dkey, filename)
    JsonDfsBackend.writeTo(path, this)
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
    SioFutureUtil
      .mapLeftSequentally(subshards, ignoreErrors=true) { _.deleteMappaings() }
      .map(_ => Unit)
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

