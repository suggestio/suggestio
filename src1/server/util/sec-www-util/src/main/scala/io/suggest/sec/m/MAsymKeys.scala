package io.suggest.sec.m

import io.suggest.es.MappingDsl
import javax.inject.Singleton
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 15:36
 * Description: Модель ключей ассиметричего шифрования/дефифрования.
 * Модель хранит в текстовом виде ключи шифрования/подписи публичные и секретные.
 * Любая возможная защита секретного ключа происходит на стороне контроллера.
 * Потом эту модель можно аккуратненько расширить пользовательской поддержкой.
 */
@Singleton
final class MAsymKeys
  extends EsModelStatic
  with MacroLogsImpl
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  override type T = MAsymKey

  override def ES_TYPE_NAME = "ak"

  // Имена полей для ключей.
  def PUB_KEY_FN = "pk"
  def SEC_KEY_FN = "sk"


  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    dsl.IndexMapping(
      source = Some( FSource(
        enabled = someTrue,
      )),
      properties = Some( Json.obj(
        PUB_KEY_FN -> FText.notIndexedJs,
        SEC_KEY_FN -> FText.notIndexedJs,
      )),
    )
  }

  private def DATA_FORMAT: OFormat[T] = (
    (__ \ PUB_KEY_FN).format[String] and
    (__ \ SEC_KEY_FN).formatNullable[String]
  )(
    (pubKey, secKeyOpt) => MAsymKey(pubKey, secKeyOpt),
    ask => (ask.pubKey, ask.secKey)
  )


  /** Вернуть JSON reads для десериализации тела документа с имеющимися метаданными. */
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    for (ask <- DATA_FORMAT) yield {
      ask.copy(
        id          = meta.id,
        versionOpt  = meta.version,
      )
    }
  }

  override def esDocWrites = DATA_FORMAT

}


final case class MAsymKey(
                           pubKey        : String,
                           secKey        : Option[String],
                           id            : Option[String]  = None,
                           versionOpt    : Option[Long]    = None,
                         )
  extends EsModelT
