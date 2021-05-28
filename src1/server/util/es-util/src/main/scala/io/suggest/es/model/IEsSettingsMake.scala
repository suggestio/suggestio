package io.suggest.es.model

import io.suggest.es.MappingDsl
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import play.api.libs.json.Json

/** Converter from any type to ES Settings instance. */
trait IEsSettingsMake[S] extends (S => Settings) {
  def apply(from: S): Settings
}


object IEsSettingsMake {

  /** Convert sio MappinDsl IndexSettings to Elasticsearch Settings. */
  implicit def indexDslToEsSettings(implicit dsl: MappingDsl): IEsSettingsMake[dsl.IndexSettings] = {
    new IEsSettingsMake[dsl.IndexSettings] {
      override def apply(from: dsl.IndexSettings): Settings = {
        Settings
          .builder()
          .loadFromSource(
            Json
              .toJson( from )
              .toString(),
            XContentType.JSON
          )
          .build()
      }
    }
  }

  implicit def dummy: IEsSettingsMake[Settings] = {
    new IEsSettingsMake[Settings] {
      override def apply(from: Settings) = from
    }
  }

}
