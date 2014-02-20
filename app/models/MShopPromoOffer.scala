package models

import util.SiowebEsUtil.client
import io.suggest.ym.model.{YmOfferDatumFields, YmPromoOfferDatum, YmOfferDatum}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.Future
import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits._
import org.elasticsearch.index.query.QueryBuilders
import EsModel.ES_INDEX_NAME
import io.suggest.ym.index.YmIndex
import io.suggest.ym.OfferTypes

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

object MShopPromoOffer {

  val ES_TYPE_NAME  = "shopPromoOffers"

  def generateMapping = YmIndex.getIndexMapping(ES_TYPE_NAME)

  def putMapping: Future[Boolean] = {
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .execute()
      .map { _.isAcknowledged }
  }

  def isMappingExists = EsModel.isMappingExists(ES_TYPE_NAME)

  /**
   * Прочитать из хранилища по id один элемент.
   * @param id id элемента.
   * @return Фьючерс с оффером, если такой имеется.
   */
  def getById(id: String): Future[Option[MShopPromoOffer]] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .execute()
      .map { getResponse =>
        if (getResponse.isExists) {
          val result = MShopPromoOffer(
            id    = Some(getResponse.getId),
            datum = YmPromoOfferDatum.fromJson(getResponse.getSourceAsMap)
          )
          Some(result)
        } else None
      }
  }

  /**
   * Найти все элементы, относящиеся к указанному магазину.
   * TODO Нужно задать сортировку, постраничный вывод и т.д.
   * @param shopId id магазина.
   * @return Фьючерс со списком результатов в неопределённом порядке.
   */
  def getAllForShop(shopId: Int): Future[Seq[MShopPromoOffer]] = {
    val shopIdQuery = QueryBuilders.fieldQuery(YmOfferDatumFields.SHOP_ID_ESFN, shopId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery)
      .execute()
      .map { searchResp =>
        searchResp.getHits.getHits.map { hit =>
          MShopPromoOffer(
            id    = Some(hit.getId),
            datum = YmPromoOfferDatum.fromJson(hit.getSource)
          )
        }
      }
  }

  /**
   * Удалить из хранилища указанный элемент.
   * @param id id элемента.
   * @return Фьючерс, содержащий истину, если всё ок, false если элемент не найден.
   */
  def deleteById(id: String): Future[Boolean] = {
    client.prepareDelete(ES_INDEX_NAME, ES_TYPE_NAME, id)
      .execute()
      .map { !_.isNotFound }
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
  id: Option[String] = None
) extends MShopSel with MShopOffersSel {

  def shop_id = datum.shopId

  /**
   * Предложить имя на основе полей.
   * @return
   */
  def anyNameIolist: Seq[_] = {
    import datum._
    datum.offerType match {
      case OfferTypes.VendorModel => List(typePrefix, vendor, model)
      case OfferTypes.ArtistTitle => List(artist, title, yearOpt.map("(" + _ + ")"))
      case _                      => List(name)
    }
  }

  /** Сохранить в хранилище текущий экземпляр данных. */
  def save: Future[MShopPromoOffer] = {
    client.prepareIndex(ES_INDEX_NAME, ES_TYPE_NAME, id getOrElse null)
      .setSource(datum.toJsonBuilder)
      .execute()
      .map {
        inxResp => MShopPromoOffer(id=Some(inxResp.getId), datum=datum)
      }
  }

  /** Удалить из хранилища модели указанный документ. */
  def delete: Future[Boolean] = {
    if (id.isEmpty) {
      Future failed new IllegalStateException("Cannot delete: document id is not defined.")
    } else {
      deleteById(id.get)
    }
  }

}


/** Межмодельный линк для моделей, содержащих поле shop-id. */
trait MShopOffersSel {
  def shop_id: Int
  def allShopOffers = getAllForShop(shop_id)
}
