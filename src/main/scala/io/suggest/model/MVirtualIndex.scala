package io.suggest.model

import io.suggest.util.SiobixFs._
import org.apache.hadoop.fs.Path
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil._
import org.apache.lucene.index.IndexNotFoundException
import io.suggest.util.{SioEsIndexUtil, LogsPrefixed, Logs}
import org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS
import io.suggest.util.SiobixFs.fs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.07.13 11:03
 * Description: Непосредственная информация об индексах в ES управляется через эту модель.
 * Шардинг ES не позволяет соотнести какой-то конкретный тип с конкретной шардой, а routing не позволяет понять,
 * в какую шарду сохранен тот или иной документ, поэтому нужны виртуальные индексы здесь.
 * Информация хранится без dkey. Таким образом, одна запись (один файл, т.е. один индекс или группа индексов) могут
 * относится к нескольким dkey.
 */
object MVirtualIndex extends Logs {

  val rootDir     = "phys_inx"
  val rootDirPath = new Path(siobix_conf_path, rootDir)

  /**
   * Вычислить путь для сохранения данных об этом индексе.
   * @param vin базовое имя.
   * @return Экземпляр Path.
   */
  def getPath(vin:String) = new Path(rootDirPath, vin)

  /**
   * Прочитать файл из хранилища.
   * @param vin базовое имя индекса.
   * @return Опциональный экземпляр сабжа.
   */
  def getForVin(vin:String): Option[MVirtualIndex] = {
    JsonDfsBackend.getAs[MVirtualIndex](getPath(vin), fs)
  }


  /**
   * Прочитать кол-во реплик для указанного виртуального индекса, состоящего из натуральных ES-индексов.
   * Выдает целое. Если перечисленные индексы имеют изменяющееся число шард, то будет экзепшен.
   * @param indices список индексов, подлежащих вычитыванию.
   * @return Кол-во реплик.
   */
  def getReplicasCountFor(indices:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Int] = {
    client.admin().cluster()
      .prepareState()
      .setFilterIndices(indices: _*)
      .setFilterMetaData(true)
      .execute()
      .flatMap { resp =>
        val md = resp.getState.getMetaData
        val results = indices map { shardName =>
          md.index(shardName) match {
            case null => throw new IndexNotFoundException(shardName)
            case imd  => imd.getSettings.getAsInt(SETTING_NUMBER_OF_REPLICAS, 1).toInt
          }
        }
        results.distinct match {
          case seq if seq.length == 1 =>
            Future.successful(seq.head)

          case _ =>
            val msg = "Unsupported variable replicas count: for shards %s => %s" format (indices, results)
            val ex  = new UnsupportedOperationException(msg)
            Future.failed(ex)
        }
      }
  }


  /**
   * Удалить эти индексы вообще.
   * @return true, если всё ок.
   */
  def deleteThese(indices:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    client.admin().indices()
      .prepareDelete(indices: _*)
      .execute()
      .map(_.isAcknowledged)
  }

  /**
   * Создать шарды.
   * @param indices индексы.
   */
  def ensureIndices(indices:Seq[String], replicasCount:Int = 1)(implicit client:Client, executor:ExecutionContext) = {
    Future.traverse(indices) { inx => createIndex(inx, replicas=replicasCount) }
  }

  /**
   * Функция генерации имени индекса из базового имени и номера шарды.
   * @param vin Virtual index name.
   * @param n Номер шарды.
   * @return Строка, которая будет использоваться как имя es-индекса.
   */
  def vinAndCounter2indexName(vin:String, n:Int): String = vin + n

}


import MVirtualIndex._

/**
 * Динамическая часть модели отражает прочитанный файл с хранимой информацией.
 * @param vin базовое имя индекса. Название индекса в ES и имя файла в DFS генерятся на основе этого.
 * @param shardCount кол-во шард
 */
case class MVirtualIndex(
  vin:        String,
  shardCount: Int = 1
) extends LogsPrefixed {

  val logPrefix = vin

  /**
   * Выдать имена всех шард.
   * @return Последовательность шард в порядке возрастания индекса.
   */
  def getShards: Seq[String] = {
    // Если шарда только одна, то она без номера-суффика.
    if (shardCount == 1) {
      Seq(vin)
    } else {
      (0 until shardCount) map { vinAndCounter2indexName(vin, _) }
    }
  }

  /**
   * Прочитать кол-во реплик из метаданных ES. Считаем для всех шард. Если число реплик у разных шард отличается,
   * то будет ошибка.
   * @return Фьючерс с кол-вом доступных шард.
   */
  def getReplicasCount(implicit client:Client, executor:ExecutionContext): Future[Int] = getReplicasCountFor(getShards)

  /**
   * Выставить новое число реплик.
   * @param replicasCount новое число реплик.
   * @return true, если всё ок. false по сути никогда и не возвращает.
   */
  def setReplicasCount(replicasCount:Int)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    SioEsIndexUtil.setReplicasCountFor(getShards, replicasCount)
  }

  /**
   * Сохранить текущий экземпляр в базу.
   * @return Сохраненный экземпляр.
   */
  def save: MVirtualIndex = {
    JsonDfsBackend.writeToPath(getPath(vin), this)
    this
  }


  /**
   * Удалить индекс вообще. И файл индекса в след за ним.
   * @return true, если всё ок. Фьючерс исполняется, когда всё сделано.
   */
  def delete(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    deleteThese(getShards) andThen { case _ =>
      fs.delete(getPath(vin), false)
    }
  }


  /**
   * Создать все необходимые для текущего экземпляра индексы.
   * @param replicasCount кол-во реплик.
   * @return
   */
  def ensureShards(replicasCount: Int = 1)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    val shards = getShards
    ensureIndices(shards, replicasCount=replicasCount)
      .flatMap { boolSeq =>
        boolSeq.distinct match {
          case s if s.length == 1 =>
            Future.successful(true)

          case other =>
            val msg = "Cannot create some of requested shards: %s => %s. Rollbacking..." format (shards, boolSeq)
            error(msg)
            val createdIndexes = (shards zip boolSeq) filter(_._2) map(_._1)
            deleteThese(createdIndexes) flatMap { _ =>
              Future.failed(new RuntimeException(msg))
            }
        }
      }
  }

  /**
   * Закрыть все шарды, выгрузив их из памяти. Типа заморозка.
   * @return Список isAcknowledged.
   */
  def close(implicit client:Client, executor:ExecutionContext): Future[Seq[Boolean]] = {
    Future.traverse(getShards) { closeIndex }
  }

  /**
   * Открыть все шарды этого индекса.
   * @return Список результатов от шард.
   */
  def open(implicit client:Client, executor:ExecutionContext): Future[Seq[Boolean]] = {
    Future.traverse(getShards) { openIndex }
  }

}