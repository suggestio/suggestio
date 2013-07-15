package io.suggest.model

import io.suggest.util.SiobixFs._
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil._
import org.apache.lucene.index.IndexNotFoundException
import org.elasticsearch.common.settings.ImmutableSettings
import io.suggest.util.{LogsPrefixed, Logs}
import org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS

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
   * @param basename базовое имя.
   * @return Экземпляр Path.
   */
  def getPath(basename:String) = new Path(rootDirPath, basename)

  /**
   * Прочитать файл из хранилища.
   * @param basename базовое имя индекса.
   * @return Опциональный экземпляр сабжа.
   */
  def getForBasename(basename:String)(implicit fs:FileSystem): Option[MVirtualIndex] = {
    JsonDfsBackend.getAs[MVirtualIndex](getPath(basename), fs)
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
      .map { resp =>
        val md = resp.getState.getMetaData
        val results = indices map { shardName =>
          md.index(shardName) match {
            case null => throw new IndexNotFoundException(shardName)
            case imd  => imd.getSettings.getAsInt(SETTING_NUMBER_OF_REPLICAS, 1).toInt
          }
        }
        results.distinct match {
          case seq if seq.length == 1 =>
            seq.head

          case _ =>
            throw new UnsupportedOperationException("Unsupported variable replicas count: for shards %s => %s" format (indices, results))
        }
      }
  }


  /**
   * Выставить кол-во реплик для указанных индексов.
   * @param indices индексы.
   * @param replicasCount новое кол-во реплик.
   * @return Удачный фьючера, если всё ок.
   */
  def setReplicasCountFor(indices:Seq[String], replicasCount:Int)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    debug("setReplicasCountFor(%s for %s) called..." format (replicasCount, indices))
    val settings = ImmutableSettings.settingsBuilder()
      .put(SETTING_NUMBER_OF_REPLICAS, replicasCount)
      .build()
    client.admin().indices()
      .prepareUpdateSettings()
      .setIndices(indices: _*)
      .setSettings(settings)
      .execute()
      .map(_ => true)
  }


  /**
   * Удалить эти индексы вообще.
   * @return true, если всё ок.
   */
  def deleteAll(indices:Seq[String])(implicit client:Client): Future[Boolean] = {
    client.admin().indices()
      .prepareDelete(indices: _*)
      .execute()
      .map(_.isAcknowledged)
  }

  /**
   * Создать шарды.
   * @param indices индексы.
   */
  def ensureIndices(indices:Seq[String], replicasCount: Int = 1)(implicit client:Client, executor:ExecutionContext) = {
    Future.traverse(indices) { inx => ensureIndex(inx, replicas=replicasCount) }
  }

}


import MVirtualIndex._

/**
 * Динамическая часть модели отражает прочитанный файл с хранимой информацией.
 * @param basename базовое имя индекса. Название индекса в ES и имя файла в DFS генерятся на основе этого.
 * @param shardCount кол-во шард
 */
case class MVirtualIndex(
  basename: String,
  shardCount: Int = 1
) extends LogsPrefixed {

  val logPrefix = basename

  /**
   * Выдать имена всех шард.
   * @return Последовательность шард в порядке возрастания индекса.
   */
  def getShards: Seq[String] = {
    if (shardCount == 1) {
      Seq(basename)
    } else {
      (0 until shardCount) map { basename + _ }
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
  def setReplicasCount(replicasCount:Int)(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    setReplicasCountFor(getShards, replicasCount)
  }

  /**
   * Сохранить текущий экземпляр в базу.
   * @return Сохраненный экземпляр.
   */
  def save(implicit fs:FileSystem): MVirtualIndex = {
    JsonDfsBackend.writeTo(getPath(basename), this)
    this
  }


  /**
   * Удалить индекс вообще.
   * @return true, если всё ок.
   */
  def delete(implicit client:Client): Future[Boolean] = deleteAll(getShards)

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
            deleteAll(createdIndexes) flatMap { _ =>
              Future.failed(new RuntimeException(msg))
            }
        }
      }
  }

}