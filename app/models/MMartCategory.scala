package models

import io.suggest.model._
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper
import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import org.elasticsearch.action.search.SearchResponse
import util.PlayMacroLogsImpl
import scala.collection.JavaConversions._
import io.suggest.model.common.{EMParentIdOpt, EMName, EMParentIdOptMut, EMNameMut}
import play.api.libs.json._
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.02.14 18:32
 * Description: Дерево пользовательских категорий конкретного ТЦ либо магазина (модель не различает владельцев).
 * Хранится в ES.
 */
object MMartCategory extends EsModelStaticT with PlayMacroLogsImpl {

  import LOGGER._

  override type T = MMartCategory

  val ES_TYPE_NAME = "usrCat"

  val YM_CAT_ESFN           = "ymCatId"
  val YM_CAT_ID_ESFN        = "ycId"
  val OWNER_ID_ESFN         = "ownerId"
  val YM_CAT_INHERIT_ESFN   = "inherit"
  val CSS_CLASS_ESFN        = "cssClass"
  val POSITION_ESFN         = "position"
  val INCLUDE_IN_ALL_ESFN   = "iia"

  /** Дефолтовый id владелеца категории. */
  val DEFAULT_OWNER_ID = "~DFLT"

  private def ownerIdQuery(ownerId: String) = {
    QueryBuilders.termQuery(OWNER_ID_ESFN, ownerId)
  }

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
   * Удалить все категории, у который ownerId != null.
   * @return Фьючерс для синхронизации.
   */
  def deleteAllOwnedNonDflt(implicit ec: ExecutionContext, client: Client): Future[_] = {
    val allQuery = QueryBuilders.matchAllQuery()
    val ownerExistsFilter = FilterBuilders.notFilter(
      FilterBuilders.termFilter(OWNER_ID_ESFN, DEFAULT_OWNER_ID)
    )
    val query = QueryBuilders.filteredQuery(allQuery, ownerExistsFilter)
    prepareDeleteByQuery
      .setQuery(query)
      .execute()
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

  /**
   * Выдать top-level категории для указанной сущности.
   * @param ownerId id сущности-владельца.
   * @return Список top-level-категорий в порядке position и затем name.
   */
  def findTopForOwner(ownerId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartCategory]] = {
    val ownerQuery = ownerIdQuery(ownerId)
    val noParentFilter = FilterBuilders.missingFilter(PARENT_ID_ESFN)
    val query = QueryBuilders.filteredQuery(ownerQuery, noParentFilter)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(query)
      .execute()
      .map { searchResp2listSortLex }
  }

  /**
   * Найти прямых потомков указанной категории.
   * @param parentCatId id категории.
   * @return Список подкатегорий без потомков.
   */
  def findDirectSubcatsOf(parentCatId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartCategory]] = {
    val query = QueryBuilders.termQuery(PARENT_ID_ESFN, parentCatId)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(query)
      .execute()
      .map { searchResp2listSortLex }
  }

  /** Узнать parentId для указанной категории.
    * @param catId id категории.
    * @return None, если нет такой категории вообще.
    *         Some(None) если parentId пуст.
    *         Some(Some(parentId)) когда есть parentId.
    */
  def getParentIdOf(catId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[Option[String]]] = {
    // TODO Следует использовать прямой доступ к полям, а не getById()
    getById(catId).map { catOpt =>
      catOpt.map { cat =>
        cat.parentId
      }
    }
  }

  /**
   * Найти категории на текущем уровне указанной категории.
   * @param catOwnerId id владельца дерева категорий. Используется если это категорий 1 уровня.
   * @param catId id текущей категории.
   * @return Nil если дерево отсуствует. Иначе Seq[MMC].
   */
  private def findAllOnSameLevel(catOwnerId: String, catId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartCategory]] = {
    getParentIdOf(catId) flatMap {
      findAllOnSameLevelParent(catOwnerId, _)
    }
  }

  private def findAllOnSameLevelParent(catOwnerId: String, maybeParentCat: Option[Option[String]])(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartCategory]] = {
    maybeParentCat match {
      // Вообще нет такой категории в хранилище. Это странная ситуация.
      case None =>
        debug(s"findAllOnSameLevelParent(own=$catOwnerId): catId not found, but it should!")
        Future successful Nil

      // Это категория верхнего уровня. Используем catOwnerId для доступа к списку категорий на текущем уровне.
      case Some(None) =>
        findTopForOwner(catOwnerId)

      // Это категория уровня >= 1. Берем id её родителя и находим категории на этом уровне.
      case Some(Some(parentCatId)) =>
        findDirectSubcatsOf(parentCatId)
    }
  }

  type CollectMMCatsAcc_t = List[(Option[String], Seq[MMartCategory])]

  /**
   * Обернуть фьючерс списка категорий во фьючерс результата-аккамулятора collectCatListsUpTo().
   * Используется для подставления выхлопа findTopForOwner() в качестве результата collectCatListsUpTo().
   * @param catsFut Фьючерс списка категорий.
   * @return Тоже самое, что и collectCatListsUpTo().
   */
  def catsAsAccFut(catsFut: Future[Seq[MMartCategory]])(implicit ec: ExecutionContext): Future[CollectMMCatsAcc_t] = {
    catsFut
      .map { mmcats => List(None -> mmcats) }
  }

  /** Рекурсивная сборка иерархии списка категорий от указанной категории наверх до самого top level.
    * Используется, чтобы сгенерить селекторы категорий на всех уровнях необходимых (начиная от top и заканчивая текущей
    * категорией). Используется восходящий траверс.
    */
  def collectCatListsUpTo(catOwnerId: String, currCatId: String, acc: CollectMMCatsAcc_t = Nil)(implicit ec: ExecutionContext, client: Client): Future[CollectMMCatsAcc_t] = {
    getParentIdOf(currCatId) flatMap {
      // Вообще None означает, что категория, на которую ссылалась карточка, была удалена и утрачена.
      // Реанимируем работу через возврат исходного списка категорий.
      case None =>
        debug(s"collectCatListsUpTo(own=$catOwnerId, catId=$currCatId): expected currCat don't exists! Recovering with top cats...")
        catsAsAccFut(findTopForOwner(catOwnerId))
      // Не-None результат означает, что можно двигаться дальше.
      case maybeParentId =>
        findAllOnSameLevelParent(catOwnerId, maybeParentId) flatMap { lCats =>
          // Нужно решить: надо ли подниматься ещё выше или это уже вершина?
          val acc1 = (Some(currCatId), lCats) :: acc
          maybeParentId match {
            case Some(Some(parentId)) =>
              collectCatListsUpTo(catOwnerId, currCatId=parentId, acc1)

            case _ => Future successful acc1
          }
        }
    }
  }

  /**
   * Асинхронно пройти категории с помощью фунцкии от текущей категории до вершины дерева.
   * @param currCatId id стартовой категории.
   * @param acc0 Начальный аккамулятор.
   * @return
   */
  def foldUpChain[AccT](currCatId: String, acc0: AccT)(f: (AccT, MMartCategory) => AccT)(implicit ec: ExecutionContext, client: Client): Future[AccT] = {
    client.prepareGet(ES_INDEX_NAME, ES_TYPE_NAME, currCatId)
      .execute()
      .flatMap { getResp =>
        if (getResp.isExists) {
          val mmc = deserializeOne(Option(getResp.getId), getResp.getSourceAsMap, rawVersion2versionOpt(getResp.getVersion))
          val acc1 = f(acc0, mmc)
          mmc.parentId match {
            case Some(parentId) => foldUpChain(parentId, acc1)(f)
            case None => Future successful acc1
          }

        } else {
          warn(s"traverseUp($currCatId): Category not found, but it should. Stopping traverse.")
          Future successful acc0
        }
      }
  }


  // TODO Надо бы заставить работать сортировку на стороне ES.
  private def searchResp2listSortLex(searchResp: SearchResponse)(implicit ec:ExecutionContext, client:Client) : Seq[MMartCategory] = {
    searchResp2list(searchResp)
      .sortBy(sortByMmcat)
  }

  def sortByMmcat(mmcat: MMartCategory) = mmcat.position + mmcat.name


  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    // Поле id компонента-владельца. По сути тут может быть любой id из любой модели, но обычно id ТЦ или id магазина.
    FieldString(OWNER_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldObject(YM_CAT_ESFN, properties = Seq(
      FieldString(YM_CAT_ID_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldBoolean(YM_CAT_INHERIT_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )),
    // Индексируем, чтобы искать в рамках под-уровня без has_child велосипеда и чтобы missing filter работал.
    FieldString(PARENT_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(POSITION_ESFN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(CSS_CLASS_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldBoolean(INCLUDE_IN_ALL_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )


  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MMartCategory(
      id        = id,
      name      = EMName.extractName(m),
      ownerId   = stringParser( m(OWNER_ID_ESFN) ),
      ymCatPtr  = JacksonWrapper.convert[MMartYmCatPtr]( m(YM_CAT_ESFN) ),
      parentId  = EMParentIdOpt.extractParentIdOpt(m),
      position  = m.get(POSITION_ESFN).fold(Int.MaxValue)(intParser),
      cssClass  = m.get(CSS_CLASS_ESFN).map(stringParser),
      includeInAll = booleanParser( m(INCLUDE_IN_ALL_ESFN) )
    )
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
final case class MMartCategory(
  name          : String,
  ymCatPtr      : MMartYmCatPtr,
  parentId      : Option[String],
  ownerId       : String = MMartCategory.DEFAULT_OWNER_ID,
  position      : Int = Int.MaxValue,
  id            : Option[String] = None,
  cssClass      : Option[String] = None,
  includeInAll  : Boolean = true
) extends EsModelEmpty with EsModelT with EMName with EMParentIdOpt with TreeSortable {

  override type T = MMartCategory
  override def companion = MMartCategory
  override def versionOpt = None

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc =
      OWNER_ID_ESFN -> JsString(ownerId) ::
      POSITION_ESFN -> JsNumber(position) ::
      INCLUDE_IN_ALL_ESFN -> JsBoolean(includeInAll) ::
      YM_CAT_ESFN -> ymCatPtr.renderPlayJson ::
      super.writeJsonFields(acc)
    if (parentId.isDefined)
      acc1 ::= PARENT_ID_ESFN -> JsString(parentId.get)
    if (cssClass.isDefined)
      acc1 ::= CSS_CLASS_ESFN -> JsString(cssClass.get)
    acc1
  }

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
final case class MMartYmCatPtr(ycId: String, inherit: Boolean = true) {
  @JsonIgnore
  def renderPlayJson: JsObject = {
    JsObject(Seq(
      YM_CAT_ID_ESFN      -> JsString(ycId),
      YM_CAT_INHERIT_ESFN -> JsBoolean(inherit)
    ))
  }
}


/** JMX MBean интерфейс */
trait MMartCategoryJmxMBean extends EsModelJMXMBeanI {
  def deleteAllOwnedNonDflt()
}

/** JMX MBean реализация. */
final class MMartCategoryJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MMartCategoryJmxMBean {
  def companion = MMartCategory

  override def deleteAllOwnedNonDflt() {
    awaitFuture(
      MMartCategory.deleteAllOwnedNonDflt
    )
  }
}
