package models

import io.suggest.ym.model.AuthInfoDatum
import MShop.ShopId_t
import EsModel._
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import org.elasticsearch.client.Client

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
    shop_id = null,
    url = null,
    auth_info = None
  )

  def applyMap(m: collection.Map[String, AnyRef], acc: MShopPriceList): MShopPriceList = {
    m foreach {
      case (SHOP_ID_ESFN, value)   => acc.shop_id = shopIdParser(value)
      case (URL_ESFN, value)       => acc.url = urlParser(value)
      case (AUTH_INFO_ESFN, value) => acc.auth_info = authInfoParser(value)
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
  var shop_id   : ShopId_t,
  var url       : String,
  var auth_info : Option[UsernamePw],
  id            : Option[String] = None
) extends EsModelT[MShopPriceList] with MShopSel {

  def companion = MShopPriceList
  def authInfoStr: Option[String] = auth_info map { _.serialize }

  override def writeJsonFields(acc: XContentBuilder) = {
    acc.startObject()
      .field(SHOP_ID_ESFN, shop_id)
      .field(URL_ESFN, url)
    if (auth_info.isDefined)
      acc.field(AUTH_INFO_ESFN, auth_info.get)
    acc.endObject()
  }

}

/** Легковесное представление пары UsernamePw для распарсенного значения колонки auth_info.
  * @param username Имя пользователя. Нельзя, чтобы в имени содержался [[models.MShopPriceList.AUTH_INFO_SEP]].
  * @param password Пароль.
  */
case class UsernamePw(username: String, password: String) {
  def toDatum = new AuthInfoDatum(username=username, password=password)
  def serialize = username + AUTH_INFO_SEP + password
}

trait ShopPriceListSel {
  def shop_id: MShop.ShopId_t
  def priceLists(implicit ec:ExecutionContext, client: Client) = getForShop(shop_id)
}

