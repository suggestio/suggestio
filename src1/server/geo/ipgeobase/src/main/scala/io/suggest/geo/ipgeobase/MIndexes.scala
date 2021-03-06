package io.suggest.geo.ipgeobase

import javax.inject.Inject
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsDyn
import org.elasticsearch.common.settings.Settings
import play.api.Configuration
import play.api.inject.Injector

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.16 17:56
  * Description: Статическая инжектируемая модель работы со скользящими time-based ES-индексами ipgeobase.
  */
object MIndexes {

  /** Константа с именем актуального алиаса ipgb-индекса.
    * val, потому что дёргается по нескольку раз даже в рамках ровно одного запроса. */
  def INDEX_ALIAS_NAME = "ipgeobase"

}


final class MIndexes @Inject() (
                                 injector   : Injector,
                               )
  extends MacroLogsDyn
{

  private def esModelConfig = injector.instanceOf[EsModelConfig]

  /**
    * Кол-во реплик для ES-индекса БД IPGeoBase.
    * Т.к. индекс очень частоиспользуемый, желательно иметь реплик не меньше, чем на обычном -sio индексе.
    *
    * Не val, т.е. часто оно надо только на dev-компе. В остальных случаях просто будет память занимать.
    */
  def REPLICAS_COUNT: Int = {
    injector
      .instanceOf[Configuration]
      .getOptional[Int]("loc.geo.ipgeobase.index.replicas_count")
      .getOrElse {
        val replicasCount = esModelConfig.ES_INDEX_REPLICAS_COUNT
        LOGGER.trace(s"REPLICAS_COUNT => $replicasCount")
        replicasCount
      }
  }

  /** Сгенерить настройки для создаваемого индекса. */
  def indexSettingsCreate: Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put( EsModelUtil.Settings.Index.REFRESH_INTERVAL,   -1 )
      .put( EsModelUtil.Settings.Index.NUMBER_OF_REPLICAS,  0 )
      .put( EsModelUtil.Settings.Index.NUMBER_OF_SHARDS,    1 )
      .build()
  }

  /** Сгенерить настройки для индекса. */
  def indexSettingsAfterBulk: Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put("index.number_of_replicas", REPLICAS_COUNT)
      .build()
  }

}
