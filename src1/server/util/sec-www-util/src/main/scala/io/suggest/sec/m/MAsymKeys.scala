package io.suggest.sec.m

import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.JacksonParsing
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Map

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
class MAsymKeys @Inject() (
  override val mCommonDi: IEsModelDiVal
)
  extends EsModelStatic
  with MacroLogsImpl
  with EsmV2Deserializer
  with EsModelPlayJsonStaticT
{

  override type T = MAsymKey

  override def ES_TYPE_NAME = "ak"

  // Имена полей для ключей.
  def PUB_KEY_FN = "pk"
  def SEC_KEY_FN = "sk"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = false)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(PUB_KEY_FN, index = false, include_in_all = false),
      FieldText(SEC_KEY_FN, index = false, include_in_all = false)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  @deprecated("Delete it", "2015.sep.07")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MAsymKey(
      id          = id,
      versionOpt  = version,
      pubKey      = JacksonParsing.stringParser( m(PUB_KEY_FN) ),
      secKey      = m.get(SEC_KEY_FN).map(JacksonParsing.stringParser)
    )
  }

  // Кешируем недособранный десериализатор экземпляров модели.
  private val _reads0 = {
    (__ \ PUB_KEY_FN).read[String] and
    (__ \ SEC_KEY_FN).readNullable[String]
  }

  /** Вернуть JSON reads для десериализации тела документа с имеющимися метаданными. */
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    _reads0 {
      (pubKey, secKeyOpt) =>
        MAsymKey(pubKey, secKeyOpt, meta.id, meta.version)
    }
  }


  override def writeJsonFields(m: T, acc0: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    var acc: FieldsJsonAcc = PUB_KEY_FN -> JsString(pubKey) :: acc0
    if (secKey.isDefined)
      acc ::= SEC_KEY_FN -> JsString(secKey.get)
    acc
  }

}



case class MAsymKey(
  pubKey        : String,
  secKey        : Option[String],
  id            : Option[String],
  versionOpt    : Option[Long] = None
)
  extends EsModelT
    with IAsymKey


/** Интерфейс для экземпляра-контейнера ассиметричного ключа. */
trait IAsymKey {
  def pubKey: String
  def secKey: Option[String]
}
