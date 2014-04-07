package io.suggest.ym.model

import io.suggest.util.JacksonWrapper
import io.suggest.model.inx2.{EsModelInx2StaticSingleT, MMartInx}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.ym.model.MShop.ShopId_t
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioConstants._
import io.suggest.ym.model.ad.AdsSearchT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.03.14 18:42
 * Description: MMartAdIndexed - экспорт-модель для MMartAd. Нужна для сохранения MMartAd при индексации.
 */

@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
object MMartAdIndexed
  extends EsModelInx2StaticSingleT[MMartAdIndexed, MMartInx]
  with AdsSearchT[MMartAdIndexed, MMartInx]
{

  val USER_CAT_STR_ESFN = "userCat.str"

  def generateMappingStaticFields = List(
    FieldAll(enabled = true, index_analyzer = EDGE_NGRAM_AN_1, search_analyzer = DFLT_AN),
    FieldSource(enabled = true)
  )

  def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_STR_ESFN, include_in_all = true, boost = Some(0.5F), index = FieldIndexingVariants.no) ::
    MMartAd.generateMappingProps
  }

  protected def dummy(id: String, inx2: MMartInx) = MMartAdIndexed(
    mmartAd = MMartAd.dummy(id),
    userCatStr = null,
    showLevels1 = null,
    inx2 = inx2
  )


  def applyKeyValue(acc: MMartAdIndexed): PartialFunction[(String, AnyRef), Unit] = {
    // Собираем partial-функцию, которая будет всё делать как надо. Чтобы типы аккамуляторов и врапперов были совместимы, тут небольшой велосипед:
    val fm = MMartAd.applyKeyValue(acc.mmartAd)
    val pf: PartialFunction[(String, AnyRef), Unit] = {
      case (USER_CAT_STR_ESFN, value)  => acc.userCatStr = JacksonWrapper.convert[List[String]](value)
      case other => fm(other)
    }
    pf
  }


  /**
   * Удалить все рекламные карточки указанного магазина.
   * @param shopId id магазина.
   * @param inx2 Метаданные об индексе.
   * @return Кол-во удалённых рядов.
   */
  def deleteByShop(shopId: ShopId_t, inx2: MMartInx)(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    client.prepareDeleteByQuery(inx2.targetEsInxName)
      .setTypes(inx2.targetEsType)
      .setQuery(MMartAd.shopSearchQuery(shopId))
      .execute()
      .map { _.iterator().size }
  }

}

import MMartAdIndexed.USER_CAT_STR_ESFN

/**
 * Экземпляр хорошо индексируемого [[MMartAd]]. Обладает полями, содержащими данные об индексе и индексируемом
 * названии категории.
 * @param mmartAd Исходный [[MMartAd]].
 * @param userCatStr Строки, собранная из названий индексируемых категорий. Используются для индексации.
 * @param showLevels1 Индексируемые уровни отображения этой карточки. Формируются на основе исходных уровней.
 * @param inx2 Данные об используемом индексе. НЕ сохраняются в БД.
 */
@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
case class MMartAdIndexed(
  mmartAd          : MMartAd,
  var userCatStr   : List[String],
  var showLevels1  : Set[AdShowLevel],
  inx2             : MMartInx
) extends MMartAdWrapperT[MMartAd] {

  override def isFieldsValid: Boolean = super.isFieldsValid && inx2 != null
  override def showLevels = showLevels1

  override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (!userCatStr.isEmpty)
      acc.array(USER_CAT_STR_ESFN, userCatStr : _*)
  }

  override protected def esIndexName: String = inx2.targetEsInxName
  override protected def esTypeName: String  = inx2.targetEsType

}




