package io.suggest.stat.m

import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.es.util.IEsClient
import org.elasticsearch.common.settings.Settings
import io.suggest.es.util.SioEsUtil.EsActionBuilderOpsExt
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

@Singleton
class MStatIndexes @Inject() (
                               injector                 : Injector,
                               esClientP                : IEsClient,
                               implicit private val ec  : ExecutionContext,
                             )
  extends MacroLogsImpl
{

  import esClientP.esClient

  /** Имя глобального алиаса. */
  def INDEX_ALIAS_NAME = "stat"

  /**
    * Кол-во реплик для ES-индекса БД IPGeoBase.
    * Т.к. индекс очень частоиспользуемый, желательно иметь реплик не меньше, чем на обычном -sio индексе.
    *
    * Не val, т.е. часто оно надо только на dev-компе. В остальных случаях просто будет память занимать.
    */
  def REPLICAS_COUNT: Int = {
    injector
      .instanceOf[Configuration]
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

  /** Кол-во шард в ново-создаваемых индексах. */
  def NUMBER_OF_SHARDS = 1

  /** Обновлять индекс каждые N секунд. */
  def INDEX_REFRESH_INTERVAL_SEC = 10


  /** Сгенерить настройки для создаваемого индекса. */
  def indexSettingsCreate: Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put( EsModelUtil.Settings.Index.REFRESH_INTERVAL,    s"${INDEX_REFRESH_INTERVAL_SEC}s")
      .put( EsModelUtil.Settings.Index.NUMBER_OF_REPLICAS,  REPLICAS_COUNT)
      .put( EsModelUtil.Settings.Index.NUMBER_OF_SHARDS,    NUMBER_OF_SHARDS)
      .build()
  }


  /**
    * Поиск и устаревших stat-индексов.
    * @return Список всех названий stat-индексов в неопределённом порядке.
    */
  def findStatIndices(): Future[Seq[String]] = {
    esClient.admin().indices()
      .prepareGetAliases()
      .addIndices( INDEX_ALIAS_NAME + EsIndexUtil.DELIM + "*" )
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
