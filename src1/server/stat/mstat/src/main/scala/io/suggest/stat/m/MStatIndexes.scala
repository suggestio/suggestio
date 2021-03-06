package io.suggest.stat.m

import javax.inject.Inject
import io.suggest.es.model._
import org.elasticsearch.common.settings.Settings
import io.suggest.util.logs.MacroLogsImpl
import play.api.{Configuration, Environment, Mode}
import play.api.inject.Injector

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 15:31
  * Description: Модель обслуживания индексов статистики.
  * Индексы v2-статистики напоминают ipgeobase, но более инертны во времени и без bulk-инициализации.
  */
object MStatIndexes {

  /** Имя глобального алиаса. */
  def INDEX_ALIAS_NAME = "stat"

  /** Кол-во шард в ново-создаваемых индексах. */
  def NUMBER_OF_SHARDS = 1

  /** Обновлять индекс каждые N секунд. */
  def INDEX_REFRESH_INTERVAL_SEC = 10

}


final class MStatIndexes @Inject() (
                                     injector                 : Injector,
                                   )
  extends MacroLogsImpl
{

  private def configuration = injector.instanceOf[Configuration]
  private lazy val esClient = injector.instanceOf[org.elasticsearch.client.Client]
  private lazy val esModel = injector.instanceOf[EsModel]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /**
    * Кол-во реплик для ES-индекса БД IPGeoBase.
    * Т.к. индекс очень частоиспользуемый, желательно иметь реплик не меньше, чем на обычном -sio индексе.
    *
    * Не val, т.е. часто оно надо только на dev-компе. В остальных случаях просто будет память занимать.
    */
  def REPLICAS_COUNT: Int = {
    configuration
      .getOptional[Int]("stat.index.replicas_count")
      .getOrElse {
        val _isProd = injector.instanceOf[Environment].mode == Mode.Prod
        val r = if (_isProd) {
          1   // Когда писался этот код, было три ноды. Т.е. одна primary шарда + две реплики.
        } else {
          0   // Нет дела до реплик на тестовой или dev-базе.
        }
        LOGGER.debug(s"REPLICAS_COUNT = $r, isProd = ${_isProd}")
        r
      }
  }


  /** Сгенерить настройки для создаваемого индекса. */
  def indexSettingsCreate: Settings = {
    val I = EsModelUtil.Settings.Index
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put( I.REFRESH_INTERVAL,    s"${MStatIndexes.INDEX_REFRESH_INTERVAL_SEC}s")
      .put( I.NUMBER_OF_REPLICAS,  REPLICAS_COUNT)
      .put( I.NUMBER_OF_SHARDS,    MStatIndexes.NUMBER_OF_SHARDS)
      // TODO Уменьшаем расход места, потребление RAM
      .build()
  }


  /**
    * Поиск и устаревших stat-индексов.
    * @return Список всех названий stat-индексов в неопределённом порядке.
    */
  def findStatIndices(): Future[Seq[String]] = {
    import esModel.api._

    esClient.admin().indices()
      .prepareGetAliases()
      .addIndices( MStatIndexes.INDEX_ALIAS_NAME + EsIndexUtil.DELIM + "*" )
      .executeFut()
      .map { resp =>
        val as = resp.getAliases
        if (as.isEmpty) {
          Nil
        } else {
          val allInxNames = as.keysIt()
            .asScala
            .toSeq
          LOGGER.trace(s"findStatIndices(): All stat indices: ${allInxNames.mkString(", ")}")
          allInxNames
        }
      }
  }

}
