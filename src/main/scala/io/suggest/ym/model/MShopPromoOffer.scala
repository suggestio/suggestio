package io.suggest.ym.model

import io.suggest.util.SioEsUtil.{DocField, Field, laFuture2sFuture}
import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.ym.index.YmIndex
import io.suggest.ym.OfferTypes
import scala.collection.Map
import org.elasticsearch.client.Client
import io.suggest.model._
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.util.MacroLogsImpl
import scala.util.{Failure, Success}

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

object MShopPromoOffer extends EsModelStaticT with MacroLogsImpl {

  import LOGGER._

  val ES_TYPE_NAME  = "shopPromoOffers"

  override type T = MShopPromoOffer

  // TODO Надо бы это запилить по-нормальному, отрефакторив YmIndex.
  override def generateMapping = YmIndex.getIndexMapping(ES_TYPE_NAME)
  def generateMappingProps: List[DocField] = ???
  def generateMappingStaticFields: List[Field] = ???

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MShopPromoOffer = {
    MShopPromoOffer(
      id    = id,
      datum = YmPromoOfferDatum.fromJson(m)
    )
  }

  /**
   * Найти все элементы, относящиеся к указанному магазину.
   * TODO Нужно задать сортировку, постраничный вывод и т.д.
   * @param shopId id магазина.
   * @return Фьючерс со списком результатов в неопределённом порядке.
   */
  def getAllForShop(shopId: String)(implicit ec:ExecutionContext, client: Client): Future[Seq[MShopPromoOffer]] = {
    val shopIdQuery = QueryBuilders.termQuery(YmOfferDatumFields.SHOP_ID_ESFN, shopId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopIdQuery)
      .execute()
      .map { searchResp2list }
  }

  /** Прочитать только shopId для указанного оффера, если такой вообще имеется.
    * @param offerId id оффера
    * @return shop_id
    */
  def getShopIdFor(offerId: String)(implicit ec:ExecutionContext, client: Client): Future[Option[String]] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, offerId)
      .setFetchSource(false)
      .setFields(YmOfferDatumFields.SHOP_ID_ESFN)
      .execute()
      .map { getResp =>
        if (getResp.isExists) {
          val value = getResp.getField(YmOfferDatumFields.SHOP_ID_ESFN).getValue
          val shopId = EsModel.stringParser(value)
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

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    // Нужно удалить картинки, ассоциированные с этой рекламой
    getById(id) flatMap {
      case Some(mspo) =>
        mspo.datum.pictures foreach { pictureId =>
          MPict.deleteFully(pictureId) onComplete {
            case Success(_)  => trace(s"deleteById($id): Deleted associated picture: $pictureId")
            case Failure(ex) => error(s"deleteById($id): Unable to delete picture: $pictureId", ex)
          }
        }
        // Асинхронно удалить текущий документ из ES.
        super.deleteById(id)

      case None => Future successful false
    }
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
) extends EsModelT with MShopOffersSel {

  override type T = MShopPromoOffer

  override def companion = MShopPromoOffer
  override def versionOpt = None
  override def shopId   = datum.shopId

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

  def toJson = datum.toJsonBuilder.string()
}


/** Межмодельный линк для моделей, содержащих поле shop-id. */
trait MShopOffersSel {
  def shopId: String
  def allShopOffers(implicit ec:ExecutionContext, client: Client) = getAllForShop(shopId)
}
