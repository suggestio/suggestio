package io.suggest.ym.model

import io.suggest.util.SioEsUtil._
import io.suggest.model._
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.ym.cat.YmCategory
import io.suggest.util.{SioFutureUtil, MacroLogsImpl}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import java.util.concurrent.atomic.AtomicInteger
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.model.common.{EMParentIdOptStaticMut, EMNameStaticMut, EMParentIdOptMut, EMNameMut}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.14 13:55
 * Description: Дерево категорий яндекс-маркета, сохраненное в elasticsearch.
 * id-шники выстроены так, чтобы их можно было сортировать. Первые два уровня имеют 1-байтовые id-шники
 * (второй уровень с префиксом). Затем идут 2-3-4-байтовые id: используется b36-кодирование через Integer.toString().
 * Формат id: l0, l1, l1l2, l1l2l3, т.е. "0", "1", "10"..."1a", "100".."10a".
 * Используется case-insensivitive-кодирование без пунктуации.
 */
object MYmCategory
  extends EsModelStaticEmpty
  with EMNameStaticMut
  with EMParentIdOptStaticMut
  with MacroLogsImpl
{
  import LOGGER._

  override type T = MYmCategory

  val ES_TYPE_NAME = "ymCat"

  /** parent-child связи между документами требуют налючия routing. Тут задаём ключ-константу для rk. */
  val ROUTING_KEY = "0"

  /** Выдать все элемены в порядке иерархии. */
  def getAllTree(implicit ec: ExecutionContext, client: Client): Future[Seq[MYmCategory]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(QueryBuilders.matchAllQuery())
      //.addSort(FIELD_ID, SortOrder.ASC)   // Почему-то не работает сортировка
      .setSize(2500)
      .execute()
      .map { searchResp2list(_).sortBy(_.id.get) }
  }

  /** Кодируется целочисленный id чтобы была максимально-короткая строка. В будущем его можно его расширить до [A-Z_-]
    * с помощью окостыливания.
    * @param i id и порядковый номер на текущем уровне в текущей ветке.
    */
  private def encodeId(i: Int) = Integer.toString(i, Character.MAX_RADIX)

  /** Залить дерево категорий в таблицу, сгенерив корректные id-шники. */
  def insertYmCats(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // Используется атомарный счетчик, чтобы генерить короткие уникальные id'шники.
    val i = new AtomicInteger(-1)
    // Вместо Future.traverse() используется mapLeftSequentally() из-за EsRejectedExecutionException: rejected execution (queue capacity 200)
    // Категории нулевого и первого уровней используют однобайтовые id-шники.
    // Инзертим категорию нулевого уровня.
    val tlCatYm = YmCategory.CAT_TREE.cats.head._2
    val tlCatId = encodeId(i.incrementAndGet())
    MYmCategory(name=tlCatYm.name, parentId=None, id=Some(tlCatId)).save.flatMap { tlCatId =>
      // Залить категории первого уровня снова с 1-байтовыми id-шниками
      SioFutureUtil.mapLeftSequentally(YmCategory.CAT_TREE_CORE.cats.values.toSeq.sortBy(_.name)) { l1Cat =>
        val l1Id = i.incrementAndGet()
        // Слишком много категорий 1-го уровня. Это опасность, надо переписывать этот кусок.
        if (l1Id > Character.MAX_RADIX) ???
        MYmCategory(name=l1Cat.name, parentId=Some(tlCatId), id=Some(encodeId(l1Id))).save.map { l1CatId =>
          l1CatId -> l1Cat
        }
      }
    } flatMap { l1Cats =>
      // Переходим к 2+-байтовой кодировке. Первые 32 prinable-байта кодировки всё равно уже почти израсходованы
      i.set(Character.MAX_RADIX)
      // Далее рекурсивно проходим по подкатегориям
      def insertYmCatTree(catTree: YmCategory.CatTreeMap_t, parentId: String): Future[_] = {
        val subcatI = new AtomicInteger(-1)
        val parentIdOpt = Some(parentId)
        SioFutureUtil.mapLeftSequentally(catTree.values.toSeq.sortBy(_.name)) { ymCat =>
          val lnId = subcatI.incrementAndGet()
          if (lnId > Character.MAX_RADIX) ???
          val id = Some(parentId + encodeId(lnId))
          val fut = MYmCategory(name=ymCat.name, parentId=parentIdOpt, id=id).save
          fut flatMap { ymCatId =>
            trace(s"'${ymCat.pathStr}' inserted as $ymCatId")
            ymCat.subcatsOpt match {
              case Some(cats) => insertYmCatTree(cats, parentId = ymCatId)
              case None       => Future successful ()
            }
          }
        }
      }
      SioFutureUtil.mapLeftSequentally(l1Cats) {
        case (l1CatId, l1Cat) => insertYmCatTree(l1Cat.subcatsOpt.get, l1CatId)
      }
    }
  }


  override protected def dummy(id: Option[String], version: Option[Long]) = {
    MYmCategory(id = id, name = null, parentId = None)
  }


  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true),
    FieldParent(ES_TYPE_NAME) // _routing включается автоматом.
  )


  /** Этой модели требуется выставлять routing для ключа, но ключ всегда один и тот же.
    * @param idOrNull id или null, если id отсутствует.
    * @return const Some(String).
    */
  override def getRoutingKey(idOrNull: String): Option[String] = someRk
  private val someRk = Some(ROUTING_KEY)
}


case class MYmCategory(
  var name      : String,
  var parentId  : Option[String],
  id            : Option[String] = None
)
  extends EsModelEmpty
  with EMNameMut
  with EMParentIdOptMut
{
  override type T = MYmCategory

  override def versionOpt = None
  override def companion = MYmCategory

  /** Дополнительные параметры сохранения можно выставить через эту функцию. */
  override def saveBuilder(irb: IndexRequestBuilder) {
    super.saveBuilder(irb)
    if (parentId.isDefined) {
      irb.setParent(parentId.get)
    }
  }

}
