package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum
import io.suggest.proto.bixo.crawler.MainProto.ShopId_t

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

  def this(shopId:ShopId_t, priceUrl:String, authInfo:Option[AuthInfoDatum]) = {
    this
    this.shopId = shopId
    this.priceUrl = priceUrl
    this.authInfo = authInfo
  }

  def this(shopId:ShopId_t, priceUrl:String, authInfoStr: String) = {
    this(shopId, priceUrl, AuthInfoDatum.parseFromString(authInfoStr))
  }


  def shopId: ShopId_t = _tupleEntry getString SHOP_ID_FN
  def shopId_=(shopId: ShopId_t) = _tupleEntry.setString(SHOP_ID_FN, shopId)

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

import MShop.ShopId_t
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
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

object MShopPriceList extends EsModelStaticT[MShopPriceList] {

  /** Разделитель имени и пароля в строке auth_info. */
  val AUTH_INFO_SEP = ":"

  val ES_TYPE_NAME = "shopPriceList"

  override def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = false, analyzer = FTS_RU_AN)
      ),
      properties = Seq(
        FieldString(
          id = SHOP_ID_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.not_analyzed
        ),
        FieldString(
          id = URL_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = AUTH_INFO_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        )
      )
    )
  }

  protected def dummy(id: String) = MShopPriceList(
    id = Some(id),
    shopId = null,
    url = null,
    authInfo = None
  )

  def applyMap(m: collection.Map[String, AnyRef], acc: MShopPriceList): MShopPriceList = {
    m foreach {
      case (SHOP_ID_ESFN, value)   => acc.shopId = shopIdParser(value)
      case (URL_ESFN, value)       => acc.url = urlParser(value)
      case (AUTH_INFO_ESFN, value) => acc.authInfo = authInfoParser(value)
    }
    acc
  }

  /**
   * Прочитать все прайс-листы, относящиеся к указанному магазину.
   * @param shopId id магазина.
   * @return Список прайслистов, относящихся к магазину.
   */
  def getForShop(shopId: ShopId_t)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShopPriceList]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .execute()
      .map { searchResp2list }
  }

  def shopIdQuery(shopId: ShopId_t) = QueryBuilders.fieldQuery(SHOP_ID_ESFN, shopId)

  def deleteByShop(shopId: ShopId_t)(implicit ec:ExecutionContext, client: Client): Future[_] = {
    client.prepareDeleteByQuery(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery(shopId))
      .execute()
  }
}


import MShopPriceList._

case class MShopPriceList(
  var shopId   : ShopId_t,
  var url      : String,
  var authInfo : Option[UsernamePw],
  id           : Option[String] = None
) extends EsModelT[MShopPriceList] with MShopSel {

  def companion = MShopPriceList
  def authInfoStr: Option[String] = authInfo map { _.serialize }

  override def writeJsonFields(acc: XContentBuilder) = {
    acc.field(SHOP_ID_ESFN, shopId)
      .field(URL_ESFN, url)
    if (authInfo.isDefined)
      acc.field(AUTH_INFO_ESFN, authInfo.get.serialize)
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
  def shopId: MShop.ShopId_t
  def priceLists(implicit ec:ExecutionContext, client: Client) = getForShop(shopId)
}

