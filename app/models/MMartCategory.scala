package models

import io.suggest.model.{TreeSortable, EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.util.JacksonWrapper
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.index.query.QueryBuilders

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.02.14 18:32
 * Description: Дерево пользовательских категорий конкретного ТЦ либо магазина (модель не различает владельцев).
 * Хранится в ES.
 */
object MMartCategory extends EsModelStaticT[MMartCategory] {

  val ES_TYPE_NAME = "usrCat"

  val YM_CAT_ESFN           = "ymCatId"
  val YM_CAT_ID_ESFN        = "ycId"
  val OWNER_ID_ESFN         = "ownerId"
  val YM_CAT_INHERIT_ESFT   = "inherit"
  val CSS_CLASS_ESFN        = "cssClass"
  val POSITION_ESFN         = "position"
  val INCLUDE_IN_ALL_ESFN   = "iia"

  private def ownerIdQuery(ownerId: String) = QueryBuilders.fieldQuery(OWNER_ID_ESFN, ownerId)

  /**
   * Получить список пользовательских категорий в неопределённом порядке.
   * @param ownerId id модели-владельца дерева категорий.
   * @param maxItems Макс.размер выдачи.
   * @return Список категорий в неопределённом порядке.
   */
  def getAllForOwner(ownerId: String, maxItems: Int = 1000)(implicit ec:ExecutionContext, client: Client): Future[Seq[MMartCategory]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(ownerIdQuery(ownerId))
      .setSize(maxItems)
      .execute()
      .map { searchResp2list }
  }


  /**
   * Выдать все категории для рендера в виде дерева.
   * @param ownerId id модели-владельца дерева категорий.
   * @return Иерархически и лексикографически отсортированный список из уровней и категорий.
   */
  def getAllTreeForOwner(ownerId: String)(implicit ec:ExecutionContext, client: Client): Future[Seq[(Int, MMartCategory)]] = {
    getAllForOwner(ownerId).map { catsUnsorted =>
      seqTreeSort(catsUnsorted.toList)
    }
  }


  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      staticFields = Seq(
        FieldAll(enabled = false, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        FieldString(
          id = NAME_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        // Поле id компонента-владельца. По сути тут может быть любой id из любой модели, но обычно id ТЦ или id магазина.
        FieldString(
          id = OWNER_ID_ESFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        ),
        FieldNestedObject(
          id = YM_CAT_ESFN,
          properties = Seq(
            FieldString(
              id = YM_CAT_ID_ESFN,
              index = FieldIndexingVariants.no,
              include_in_all = false
            ),
            FieldBoolean(
              id = YM_CAT_INHERIT_ESFT,
              index = FieldIndexingVariants.no,
              include_in_all = false
            )
          )
        ),
        FieldString(
          id = PARENT_ID_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = POSITION_ESFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = CSS_CLASS_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = INCLUDE_IN_ALL_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        )
      )
    )
  }

  def applyKeyValue(acc: MMartCategory): PartialFunction[(String, AnyRef), Unit] = {
    case (NAME_ESFN, value)         => acc.name = stringParser(value)
    case (OWNER_ID_ESFN, value)     => acc.ownerId = stringParser(value)
    case (YM_CAT_ESFN, value)       => acc.ymCatPtr = JacksonWrapper.convert[MMartYmCatPtr](value)
    case (PARENT_ID_ESFN, value)    => acc.parentId = Some(stringParser(value))
    case (POSITION_ESFN,  value)    => acc.position = intParser(value)
    case (CSS_CLASS_ESFN, value)    => acc.cssClass = Some(stringParser(value))
    case (INCLUDE_IN_ALL_ESFN, value) => acc.includeInAll = booleanParser(value)
  }

  protected def dummy(id: String) = {
    MMartCategory(id = Some(id), name = null, ymCatPtr = null, ownerId = null, parentId = None, position = Int.MaxValue)
  }
}

import MMartCategory._

/**
 * Экземпляр модели.
 * @param name Отображаемое название категории. Наверное, Будет прогонятся через Messages()
 * @param ymCatPtr Данные о backend-категории яндекс-маркета: id и прочие параметры.
 * @param ownerId id компонента-владельца. Тут может быть id ТЦ или магазина - эта модель их не различает.
 * @param parentId id родительской категории [[MMartCategory]].
 * @param position Позиция в выдаче. Используется при сортировке.
 * @param cssClass Класс css.
 * @param includeInAll Индексировать текущую категорию вместе с товаром.
 * @param id Идентификатор категории.
 */
case class MMartCategory(
  var name      : String,
  var ownerId   : String,
  var ymCatPtr  : MMartYmCatPtr,
  var parentId  : Option[String],
  var position  : Int = Int.MaxValue,
  var id        : Option[String] = None,
  var cssClass  : Option[String] = None,
  var includeInAll: Boolean = true
) extends EsModelT[MMartCategory] with TreeSortable {

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(NAME_ESFN, name)
      .field(OWNER_ID_ESFN, ownerId)
      .field(POSITION_ESFN, position)
      .field(INCLUDE_IN_ALL_ESFN, includeInAll)
    ymCatPtr.render(acc)
    if (parentId.isDefined)
      acc.field(PARENT_ID_ESFN, parentId.get)
    if (cssClass.isDefined)
      acc.field(CSS_CLASS_ESFN, cssClass.get)
  }

  def companion = MMartCategory

  /**
   * Сохранить экземпляр в хранилище ES.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    if (ownerId == null || name == null || ymCatPtr == null || parentId == null) {
      throw new IllegalStateException("Save impossible: some or all of mandatory field not set: " + this)
    } else {
      super.save
    }
  }
}

/**
 * @param ycId id категории в дереве ym-категорий.
 * @param inherit Наследовать прямые подкатегории ym-категории при поиске.
 */
case class MMartYmCatPtr(ycId: String, inherit: Boolean = true) {
  def render(acc: XContentBuilder) {
    acc.startObject(YM_CAT_ESFN)
      .field(YM_CAT_ID_ESFN, ycId)
      .field(YM_CAT_INHERIT_ESFT, inherit)
    .endObject()
  }
}

