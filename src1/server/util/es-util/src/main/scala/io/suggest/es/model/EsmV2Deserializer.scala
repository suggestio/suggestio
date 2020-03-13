package io.suggest.es.model

import io.suggest.util.logs.IMacroLogs
import play.api.libs.json.{Json, Reads}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:23
 * Description: Аддон для воплощения immutable-десериализации на базе play.json и тайпклассов.
 */
trait EsmV2Deserializer extends EsModelCommonStaticT with IMacroLogs {

  /**
   * Вернуть JSON Reads для десериализации тела документа с переданными метаданными.
   * @param meta Метаданные ES-документа, необходимые для сборки моделей.
   * @return Инстанс Reads[T]
   */
  protected def esDocReads(meta: IEsDocMeta): Reads[T]

  override def deserializeOne2[D](doc: D)(implicit ev: IEsDoc[D]): T = {
    // Готовим метаданные документа для вызова сборки и исполнения Reads-десериализатора.
    val meta = EsDocMeta(doc)
    // Получаем десериализатор.
    val reader = esDocReads(meta)

    val parseResult = Json
      .parse( ev.bodyAsString(doc) )
      .validate(reader)

    if (parseResult.isError)
      LOGGER.error(s"Failed to parse JSON of $ES_TYPE_NAME/${ev.idOrNull(doc)}:\n ${ev.bodyAsString(doc)}\n $parseResult")
    // Надо бы предусмотреть возможность ошибки десериализации...
    parseResult.get
  }

}
