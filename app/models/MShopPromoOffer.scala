package models

import util.SiowebEsUtil.client
import io.suggest.ym.model.{YmOfferDatumFields, YmPromoOfferDatum}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.QueryBuilders
import EsModel.ES_INDEX_NAME
import io.suggest.ym.index.YmIndex
import io.suggest.ym.OfferTypes
import models.MShop.ShopId_t
import scala.collection.Map
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:39
 * Description: Промо-оффер, т.е. неточное коммерческое предложение магазина, управляемое вручную продавцами,
 * и управляется оно именно через эту модель.
 * Из-за сильной абстрактности схемы модели товара/услуги и по ряду других причин используется schema-free-хранилище
 * в отдельном индексе ES. Для id используются стандартные id-шники ES.
 * Этот оффер привязан к магазину -- это избавляет от кучи головной боли, хоть и бывает не оптимально.
 */

object MShopPromoOffer extends EsModelMinimalStaticT[MShopPromoOffer] {

  val ES_TYPE_NAME  = "shopPromoOffers"

  def generateMapping = YmIndex.getIndexMapping(ES_TYPE_NAME)

  override def deserializeOne(id: String, m: Map[String, AnyRef]): MShopPromoOffer = {
    MShopPromoOffer(
      id    = Some(id),
      datum = YmPromoOfferDatum.fromJson(m)
    )
  }

  /**
   * Найти все элементы, относящиеся к указанному магазину.
   * TODO Нужно задать сортировку, постраничный вывод и т.д.
   * @param shopId id магазина.
   * @return Фьючерс со списком результатов в неопределённом порядке.
   */
  def getAllForShop(shopId: MShop.ShopId_t)(implicit ec:ExecutionContext): Future[Seq[MShopPromoOffer]] = {
    val shopIdQuery = QueryBuilders.fieldQuery(YmOfferDatumFields.SHOP_ID_ESFN, shopId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery)
      .execute()
      .map { searchResp2list }
  }

   /** Прочитать только shopId для указанного оффера, если такой вообще имеется. */
  def getShopIdFor(offerId: String)(implicit ec:ExecutionContext): Future[Option[ShopId_t]] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, offerId)
      .setFields(YmOfferDatumFields.SHOP_ID_ESFN)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          val value = getResp.getField(YmOfferDatumFields.SHOP_ID_ESFN).getValue
          val shopId = EsModel.shopIdParser(value)
          Some(shopId)
        } else None
      }
  }

  private def optSeq2string(tokens: Option[Any]*): String = {
    tokens.foldLeft (new StringBuilder) {
      case (sb, Some(t))  => sb.append(t.toString).append(' ')
      case (sb, None)     => sb
    }.toString()
  }

}

import MShopPromoOffer._

/**
 * Экземпляр оффера в рамках web21.
 * @param datum Cascading-модель данных оффера. По сути как бы backend этой модели.
 *              При чтении нижележащая модель парсит json из ES, заполняя свои поля.
 * @param id Рандомный id из ES, задаётся им же при инзерте. Т.е. обычно это строка base64(uuid()).
 */
case class MShopPromoOffer(
  // TODO Нужно использовать облегчённую модель датума, без постоянной сериализации-десериализации. Можно просто через набор одноимённых var + парсер json в над-трейте.
  datum: YmPromoOfferDatum = new YmPromoOfferDatum(),
  var id: Option[String] = None
) extends EsModelMinimalT[MShopPromoOffer] with MShopSel with MShopOffersSel {

  def companion = MShopPromoOffer
  def shop_id   = datum.shopId

  /**
   * Предложить имя товара на основе указанных полей.
   * @return
   */
  def anyNameIolist: Seq[_] = {
    import datum._
    datum.offerType match {
      case OfferTypes.VendorModel => List(typePrefix, " ", vendor, " ", model)
      case OfferTypes.ArtistTitle => List(artist, " ", title, yearOpt.map(" (" + _ + ")"))
      case _                      => List(name)
    }
  }

  def toJson: XContentBuilder = datum.toJsonBuilder
}


/** Межмодельный линк для моделей, содержащих поле shop-id. */
trait MShopOffersSel {
  def shop_id: MShop.ShopId_t
  def allShopOffers(implicit ec:ExecutionContext) = getAllForShop(shop_id)
}
