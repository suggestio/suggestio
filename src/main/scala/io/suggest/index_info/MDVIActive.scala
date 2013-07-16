package io.suggest.index_info

import io.suggest.model.MDkeyVirtualIndex.activeDirRelPath
import org.apache.hadoop.fs.{FileSystem, Path}
import io.suggest.util.SiobixFs.dkeyPathConf
import io.suggest.model.{MVirtualIndex, SioSearchContext, JsonDfsBackend}
import org.joda.time.LocalDate
import io.suggest.util.Logs
import io.suggest.util.DateParseUtil.toDaysCount
import scala.concurrent.Future
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.07.13 12:01
 * Description: Model Dkey Virtual Index Active - модель хранения активных связей между dkey и виртуальными шардами.
 */

object MDVIActive {

  /**
   * Сгенерить путь для dkey куда сохраняются данные этой модели.
   * @param dkey ключ домена.
   * @return путь.
   */
  def getDkeyPath(dkey:String) = new Path(dkeyPathConf(dkey), activeDirRelPath)

  /**
   * Прочитать из хранилища и распарсить json-сериализованные данные по сабжу.
   * @param dkey Ключ домена.
   * @param vinName Название индекса.
   * @return Опциональный сабж.
   */
  def getForDkeyVin(dkey:String, vinName:String)(implicit fs:FileSystem): Option[MDVIActive] = {
    JsonDfsBackend.getAs[MDVIActive](getDkeyPath(dkey), fs)
  }

}


import MDVIActive._

/**
 * Класс верхнего уровня. Хранит информацию по виртуальному индексу в контексте текущего dkey.
 * @param dkey ключ домена
 * @param vin имя нижележащего виртуального индекса.
 * @param subshards Список подшард.
 * @param generation Поколение. При миграции в другой индекс поколение инкрементируется.
 */
case class MDVIActive(
  dkey:       String,
  vin:        String,
  subshards:  List[MDkeyVirtualSubshard],
  generation: Int = 0

) extends Logs {

  // TODO Когда тут всё стабилизируется, надо сделать extract interface. Тогда можно будет создавать merge-индексы,
  //      позволяя юзерам объединять домены.

  // типа как бы id всего DkeyIndex.
  lazy val id = dkey + "$" + vin

  /**
   * Нужно сохранить документ. И встает вопрос: в какую именно подшарду.
   * @param d дата документа.
   * @return подшарду для указанной даты. Из неё можно получить название типа при необходимости.
   */
  def getSubshardForDate(d:LocalDate): MDkeyVirtualSubshard = {
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
  def getAllTypesForShard(shardN: Int): List[MDkeyVirtualSubshard] = {
    subshards.foldLeft[List[MDkeyVirtualSubshard]] (Nil) { (acc, ss) =>
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
    val path = getDkeyPath(dkey)
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
   * @param indices список индексов. По дефолту запрашивается у virtual-индекса.
   * @param failOnError Сдыхать при ошибке. По дефолту true.
   * @return true, если всё хорошо.
   */
  def setMappings(indices:Seq[String] = getShards, failOnError:Boolean = true)(implicit client:Client): Future[Boolean] = {
    // Запустить парралельно загрузку маппингов для всех типов (всех подшард).
    Future.traverse(subshards) {
      _.setMappings(indices, failOnError)
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
   * @param optimize Вызывать ли автоматически чистку индекса от удаленных данных? По дефолту = нет.
   * @return
   */
  def deleteMappings(optimize:Boolean = false)(implicit client:Client): Future[Boolean] = {

  }

}

