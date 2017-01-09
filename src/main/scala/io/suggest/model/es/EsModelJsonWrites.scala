package io.suggest.model.es

import play.api.libs.json.Writes

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:42
 * Description: Поддержка JSON-сериализации экземпляров модели с помощью компаньона
 * и play json writes внутри него.
 */

/** Интерфейс для доступа к play.json-сериализатору модели. */
trait EsModelJsonWrites extends EsModelCommonStaticT {

  def esDocWrites: Writes[T]

  override def toJson(m: T): String = {
    esDocWrites
      .writes(m)
      .toString()
  }

}
