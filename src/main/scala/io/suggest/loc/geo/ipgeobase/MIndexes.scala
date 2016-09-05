package io.suggest.loc.geo.ipgeobase

import java.time.ZonedDateTime

import com.google.inject.{Inject, Singleton}
import io.suggest.model.es.IEsModelDiVal
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.ResourceNotFoundException
import org.elasticsearch.common.settings.Settings

import scala.concurrent.Future
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.16 17:56
  * Description: Модель работы со скользящими time-based индексами ipgeobase.
  */

object MIndexes {

  /** Константа с именем актуального алиаса ipgb-индекса. */
  def INDEX_ALIAS_NAME = "ipgeobase"

  /** Сборка нового имени для нового индекса. */
  def newIndexName(): String = {
    val now = ZonedDateTime.now()
    s"$INDEX_ALIAS_NAME-${now.getYear}${now.getMonthValue}${now.getDayOfMonth}-${now.getHour}${now.getMinute}${now.getSecond}"
  }

}


/** Статическая инжектируемая модель управления ES-индексами IpGeoBase. */
@Singleton
class MIndexes @Inject() (
  mCommonDi: IEsModelDiVal
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import LOGGER._
  import MIndexes._


  /**
    * Кол-во реплик для ES-индекса БД IPGeoBase.
    * Т.к. индекс очень частоиспользуемый, желательно иметь реплик не меньше, чем на обычном -sio индексе.
    *
    * Не val, т.е. часто оно надо только на dev-компе. В остальных случаях просто будет память занимать.
    */
  def REPLICAS_COUNT: Int = {
    configuration.getInt("loc.geo.ipgeobase.index.replicas_count").getOrElse {
      val _isProd = mCommonDi.isProd
      val r = if (_isProd) {
        2   // Когда писался этот код, было три ноды. Т.е. одна primary шарда + две реплики.
      } else {
        0   // Нет дела до реплик на тестовой или dev-базе.
      }
      debug(s"REPLICAS_COUNT = $r, isProd = ${_isProd}")
      r
    }
  }

  /** Выставить алиас на текущий индекс. */
  def installIndexAlias(newIndexName: String): Future[_] = {
    val aliasName = INDEX_ALIAS_NAME
    val fut = esClient.admin().indices()
      .prepareAliases()
      // Удалить все алиасы с необходимым именем.
      .removeAlias("*", aliasName)
      // Добавить алиас на новый индекс.
      .addAlias(newIndexName, aliasName)
      .execute()

    // Подключить логгирование к работе...
    lazy val logPrefix = s"installIndexAliase($newIndexName <= $aliasName):"
    fut.onComplete {
      case Success(r)  => debug(s"$logPrefix OK, result = $r")
      case Failure(ex) => error(s"$logPrefix Failed to update index alias $aliasName", ex)
    }

    fut
  }


  /** Узнать имя индекса, сокрытого за глобальным алиасом INDEX_ALIAS_NAME. */
  def getAliasedIndexName(): Future[Set[String]] = {
    val fut = esClient.admin().indices()
      .prepareGetAliases(INDEX_ALIAS_NAME)
      .execute()

    val logPrefix = "getAliasesIndexName():"
    fut
      .map { resp =>
        trace(s"$logPrefix Ok, found ${resp.getAliases.size()} indexes.")
        resp.getAliases
          .keysIt()
          .toSet
      }
      .recover { case ex: ResourceNotFoundException =>
        // Если алиасов не найдено, ES обычно возвращает ошибку 404. Это тоже отработать надо бы.
        warn(s"$logPrefix 404, suppressing error to empty result.", ex)
        Set.empty
      }
  }


  /** Собрать новый индекс для заливки туда моделей ipgeobase. */
  def createIndex(newIndexName: String): Future[_] = {
    val fut: Future[_] = {
      esClient.admin().indices()
        .prepareCreate(newIndexName)
        // Надо сразу отключить index refresh в целях оптимизации bulk-заливки в индекс.
        .setSettings( indexSettingsCreate() )
        .execute()
    }

    lazy val logPrefix = s"createIndex($newIndexName):"
    fut.onComplete {
      case Success(res) => debug(s"$logPrefix Ok, $res")
      case Failure(ex)  => error(s"$logPrefix failed", ex)
    }

    fut
  }

  /** Сгенерить настройки для индекса. */
  def indexSettingsCreate(): Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put("index.refresh",  -1)
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build()
  }

  /** Сгенерить настройки для индекса. */
  def indexSettingsReady(): Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put("index.number_of_replicas", REPLICAS_COUNT)
      .build()
  }

  /** Логика удаления старого ненужного индекса. */
  def deleteIndex(oldIndexName: String): Future[_] = {
    val fut: Future[_] = esClient.admin().indices()
      .prepareDelete(oldIndexName)
      .execute()

    val logPrefix = s"deleteIndex($oldIndexName):"

    // Отрабатывать ситуацию, когда индекс не найден.
    val fut1 = fut.recover { case ex: ResourceNotFoundException =>
      debug(s"$logPrefix Looks like, index not exist, already deleted?", ex)
    }

    // Логгировать завершение команды.
    fut1.onComplete {
      case Success(res) => debug(s"$logPrefix Index deleted ok: $res")
      case Failure(ex)  => error(s"$logPrefix Failed to delete index", ex)
    }

    fut1
  }


  /** Когда заливка данных закончена, выполнить подготовку индекса к эсплуатации.
    * elasticsearch 2.0+: переименовали операцию optimize в force merge. */
  def optimizeAfterBulk(newIndexName: String): Future[_] = {
    val startedAt = System.currentTimeMillis()

    // Запустить оптимизацию всего ES-индекса.
    val inxOptFut: Future[_] = {
      esClient.admin().indices()
        .prepareForceMerge(newIndexName)
        .setMaxNumSegments(1)
        .setFlush(true)
        .execute()
    }

    lazy val logPrefix = s"optimizeAfterBulk($newIndexName):"

    // Потом нужно выставить не-bulk настройки для готового к работе индекса.
    val inxSettingsFut = inxOptFut.flatMap { _ =>
      val updInxSettingsAt = System.currentTimeMillis()
      val fut2: Future[_] = {
        esClient.admin().indices()
          .prepareUpdateSettings(newIndexName)
          .setSettings( indexSettingsReady() )
          .execute()
      }

      trace(s"$logPrefix Optimize took ${updInxSettingsAt - startedAt} ms")
      for (_ <- fut2)
        trace(s"$logPrefix Update index settings took ${System.currentTimeMillis() - updInxSettingsAt} ms.")

      fut2
    }

    inxSettingsFut
  }

}
