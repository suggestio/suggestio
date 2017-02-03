package io.suggest.loc.geo.ipgeobase

import com.google.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.common.settings.Settings

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.16 17:56
  * Description: Статическая инжектируемая модель работы со скользящими time-based ES-индексами ipgeobase.
  */

@Singleton
class MIndexes @Inject() (
  override val mCommonDi: IEsModelDiVal
)
  extends EsIndexStaticAlias
  with EsIndexDynCreate
  with EsIndexDynDelete
  with EsIndexOptimizeAfterBulk
  with MacroLogsImpl
{

  import mCommonDi._
  import LOGGER._


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

  /** Константа с именем актуального алиаса ipgb-индекса.
    * val, потому что дёргается по нескольку раз даже в рамках ровно одного запроса. */
  override val INDEX_ALIAS_NAME = "ipgeobase"

  /** Сгенерить настройки для создаваемого индекса. */
  override def indexSettingsCreate: Settings = {
    Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put("index.refresh",  -1)
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build()
  }

  /** Сгенерить настройки для индекса. */
  override def indexSettingsAfterBulk: Option[Settings] = {
    val settings = Settings.builder()
      // Индекс ipgeobase не обновляется после заливки, только раз в день полной перезаливкой. Поэтому refresh не нужен.
      .put("index.number_of_replicas", REPLICAS_COUNT)
      .build()
    Some(settings)
  }

}
