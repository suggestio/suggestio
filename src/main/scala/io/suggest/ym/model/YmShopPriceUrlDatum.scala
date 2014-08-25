package io.suggest.ym.model

import io.suggest.util.{MacroLogsImpl, CascadingFieldNamer}
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.02.14 13:55
 * Description: Для хранения и представления информации о расположении прайсов на сайтах используется этот датум.
 */
object YmShopPriceUrlDatum extends CascadingFieldNamer with Serializable {

  val SHOP_ID_FN    = fieldName("shopId")
  val PRICE_URL_FN  = fieldName("priceUrl")
  val AUTH_INFO_FN  = fieldName("authInfo")

  val FIELDS = new Fields(SHOP_ID_FN, PRICE_URL_FN, AUTH_INFO_FN)


  /** Десериализация кортежа. */
  val deserializeAuthInfo: PartialFunction[AnyRef, Option[AuthInfoDatum]] = {
    case null     => None
    case t: Tuple => Some(AuthInfoDatum(t))
  }

  /** Сериализовать auth info в значение для поля AUTH_INFO_FN. */
  def serializeAuthInfo(aid: Option[AuthInfoDatum]): Tuple = {
    if (aid == null || aid.isEmpty || aid.get == null) {
      null
    } else {
      aid.get.getTuple
    }
  }

}

import YmShopPriceUrlDatum._

class YmShopPriceUrlDatum extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(shopId:String, priceUrl:String, authInfo:Option[AuthInfoDatum]) = {
    this
    this.shopId = shopId
    this.priceUrl = priceUrl
    this.authInfo = authInfo
  }

  def this(shopId:String, priceUrl:String, authInfoStr: String) = {
    this(shopId, priceUrl, AuthInfoDatum.parseFromString(authInfoStr))
  }


  def shopId: String = _tupleEntry getString SHOP_ID_FN
  def shopId_=(shopId: String) = _tupleEntry.setString(SHOP_ID_FN, shopId)

  def priceUrl = _tupleEntry getString PRICE_URL_FN
  def priceUrl_=(priceUrl: String) = _tupleEntry.setString(PRICE_URL_FN, priceUrl)

  def authInfo = {
    val maybeDatum = _tupleEntry.getObject(AUTH_INFO_FN)
    deserializeAuthInfo(maybeDatum)
  }
  def authInfo_=(aid: Option[AuthInfoDatum]) = _tupleEntry.setObject(AUTH_INFO_FN, serializeAuthInfo(aid))
}


// Далее - не-datum реализация этой же модели. Для random-access вне flow, т.е. для веб-морды например.
// И сериализация тут не предусмотрена.

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.model.EsModel._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.02.14 18:46
 * Description: Таблица хранит адреса прайс-листов магазинов. Модель является хранилищем для
 * [[io.suggest.ym.model.YmShopPriceUrlDatum]].
 */
object MShopPriceList extends EsModelStaticT with MacroLogsImpl {

  override type T = MShopPriceList

  /** Разделитель имени и пароля в строке auth_info. */
  val AUTH_INFO_SEP = ":"

  val ES_TYPE_NAME = "shopPriceList"


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(SHOP_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
    FieldString(URL_ESFN, include_in_all = false, index = FieldIndexingVariants.no),
    FieldString(AUTH_INFO_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
  )


  override protected def dummy(id: Option[String], version: Option[Long]) = MShopPriceList(
    id = id,
    shopId = null,
    url = null,
    authInfo = None
  )


  def applyKeyValue(acc: MShopPriceList): PartialFunction[(String, AnyRef), Unit] = {
    case (SHOP_ID_ESFN, value)      => acc.shopId = stringParser(value)
    case (URL_ESFN, value)          => acc.url = urlParser(value)
    case (AUTH_INFO_ESFN, value)    => acc.authInfo = authInfoParser(value)
  }

  /**
   * Прочитать все прайс-листы, относящиеся к указанному магазину.
   * @param shopId id магазина.
   * @return Список прайслистов, относящихся к магазину.
   */
  def getForShop(shopId: String)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShopPriceList]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .execute()
      .map { searchResp2list }
  }

  def shopIdQuery(shopId: String) = QueryBuilders.termQuery(SHOP_ID_ESFN, shopId)

  def deleteByShop(shopId: String)(implicit ec:ExecutionContext, client: Client): Future[_] = {
    client.prepareDeleteByQuery(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .execute()
  }
}


import MShopPriceList._

case class MShopPriceList(
  var shopId   : String,
  var url      : String,
  var authInfo : Option[UsernamePw],
  id           : Option[String] = None
) extends EsModelT {

  override type T = MShopPriceList

  override def companion = MShopPriceList
  override def versionOpt = None

  def authInfoStr: Option[String] = authInfo map { _.serialize }

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc = {
      SHOP_ID_ESFN -> JsString(shopId) ::
      URL_ESFN -> JsString(url) ::
      acc
    }
    if (authInfo.isDefined)
      acc1 ::= AUTH_INFO_ESFN -> JsString(authInfo.get.serialize)
    acc1
  }

}

/** Легковесное представление пары UsernamePw для распарсенного значения колонки auth_info.
  * @param username Имя пользователя. Нельзя, чтобы в имени содержался [[MShopPriceList.AUTH_INFO_SEP]].
  * @param password Пароль.
  */
case class UsernamePw(username: String, password: String) {
  def toDatum = new AuthInfoDatum(username=username, password=password)
  def serialize = username + AUTH_INFO_SEP + password
}

trait ShopPriceListSel {
  def shopId: String
  def priceLists(implicit ec:ExecutionContext, client: Client) = getForShop(shopId)
}

