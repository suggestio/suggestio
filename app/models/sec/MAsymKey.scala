package models.sec

import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
object MAsymKey extends EsModelStaticT with PlayMacroLogsImpl with CurriedPlayJsonEsDocDeserializer {

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
      FieldString(PUB_KEY_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(SEC_KEY_FN, index = FieldIndexingVariants.no, include_in_all = false)
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
      pubKey      = EsModel.stringParser( m(PUB_KEY_FN) ),
      secKey      = m.get(SEC_KEY_FN).map(EsModel.stringParser)
    )
  }

  /** play-json-маппер, возвращающий функцию, собирающий итоговый элемент.
    * curried-фунцкия должна получить на вход опциональные id и выверенную version,
    * и возвращать экземпляр модели. */
  override protected def esDocReads: Reads[Reads_t] = (
    (__ \ PUB_KEY_FN).read[String] and
    (__ \ SEC_KEY_FN).readNullable[String]
  ) {
    (pubKey, secKeyOpt) =>
      apply(pubKey, secKeyOpt, _, _)
  }

}


import MAsymKey._


case class MAsymKey(
  pubKey        : String,
  secKey        : Option[String],
  id            : Option[String],
  versionOpt    : Option[Long] = None
) extends EsModelT with EsModelPlayJsonT with IAsymKey {

  override def companion = MAsymKey

  override type T = MAsymKey

  override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc = PUB_KEY_FN -> JsString(pubKey) :: acc0
    if (secKey.isDefined)
      acc ::= SEC_KEY_FN -> JsString(secKey.get)
    acc
  }

}

/** Интерфейс для экземпляра-контейнера ассиметричного ключа. */
trait IAsymKey {
  def pubKey: String
  def secKey: Option[String]
}
