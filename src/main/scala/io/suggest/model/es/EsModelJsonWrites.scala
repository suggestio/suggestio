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
trait IEsDocJsonWrites extends EsModelCommonStaticT {
  def esDocWrites: Writes[T]
}

/** Аддон для сериализации через play.json.Writes, доступного в компаньоне. */
trait EsModelJsonWrites extends EsModelCommonT {

  override def companion: IEsDocJsonWrites { type T = T1 }

  override def toJson: String = {
    companion.esDocWrites
      .writes(thisT)
      .toString()
  }
}
