package models

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelT, EsModelPlayJsonT, EsModel, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.json.JsString
import util.PlayMacroLogsImpl
import play.api.Play.{current, configuration}

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:55
 * Description: Модель для хранения ошибок на клиентах. Информация имеет TTL, настраив
 */
object MRemoteError extends EsModelStaticT  with PlayMacroLogsImpl {

  override type T = MRemoteError

  override val ES_INDEX_NAME = EsModel.GARBAGE_INDEX
  override val ES_TYPE_NAME = "rerr"

  // Имена полей. По возможности должны совпадать с названиями в MAdStat.
  val MSG_ESFN            = "msg"
  val URL_ESFN            = "url"
  val TIMESTAMP_ESFN      = "timestamp"
  val CLIENT_ADDR_ESFN    = "clientAddr"
  val UA_ESFN             = "ua"

  /** Время хранения данных в модели. */
  val TTL_DAYS = configuration.getInt("model.remote.error.store.ttl.days") getOrElse 30

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = true),
      FieldSource(enabled = true),
      FieldTtl(enabled = true, default = s"${TTL_DAYS}d")
    )
  }

  /**
   * Индексируем всё, т.к. модель ориентирована на быстрое исправление ошибок.
   * Мусор из индекса будет вычищен по TTL.
   * @return
   */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(MSG_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldDate(TIMESTAMP_ESFN, index = null, include_in_all = true),
      FieldString(CLIENT_ADDR_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(UA_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    def parseStr(fn: String) = m.get(fn).fold("")(EsModel.stringParser)
    MRemoteError(
      msg         = parseStr(MSG_ESFN),
      url         = parseStr(URL_ESFN),
      clientAddr  = parseStr(CLIENT_ADDR_ESFN),
      ua          = parseStr(UA_ESFN),
      timestamp   = m.get(TIMESTAMP_ESFN).fold(DateTime.now)(EsModel.dateTimeParser),
      id          = id
    )
  }

}


import MRemoteError._


case class MRemoteError(
  msg         : String,
  url         : String,
  clientAddr  : String,
  ua          : String,
  timestamp   : DateTime = DateTime.now,
  id          : Option[String] = None
) extends EsModelT with EsModelPlayJsonT {

  override type T = MRemoteError

  override def companion = MRemoteError

  override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    MSG_ESFN            -> JsString(msg) ::
      URL_ESFN          -> JsString(url) ::
      TIMESTAMP_ESFN    -> EsModel.date2JsStr(timestamp) ::
      CLIENT_ADDR_ESFN  -> JsString(clientAddr) ::
      UA_ESFN           -> JsString(ua) ::
      acc0
  }

  /** Версия тут не нужна, т.к. модель write-only, и читается через kibana. */
  override def versionOpt: Option[Long] = None
}

