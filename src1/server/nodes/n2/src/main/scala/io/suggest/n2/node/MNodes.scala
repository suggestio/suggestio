package io.suggest.n2.node

import java.time.OffsetDateTime
import io.suggest.adn.edit.m.MAdnResView
import io.suggest.cal.m.MCalType

import javax.inject.{Inject, Singleton}
import io.suggest.n2.bill.MNodeBilling
import io.suggest.n2.edge.{MEdge, MNodeEdges}
import io.suggest.n2.extra.{MNodeCalendar, MNodeExtras}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.event.{MNodeDeleted, MNodeSaved}
import io.suggest.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model._
import io.suggest.es.search.EsDynSearchStatic
import io.suggest.event.ISioNotifierStaticClient
import io.suggest.jd.MJdEdgeId
import io.suggest.util.logs.MacroLogsImpl
import monocle.Traversal
import monocle.macros.GenLens
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.inject.Injector
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.std.option._

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 14:04
 * Description: Модель одного "Узла" графа N2 с большой буквы.
 *
 * Архитектура "N2" с этой моделью в центре появилась в ходе принятого решения объеденить
 * ADN-узлы, карточки, теги, юзеры и прочие сущности в единую модель узлов графа.
 *
 * Есть также ребра (поле edges), описанные моделями [[io.suggest.n2.edge.MEdge]].
 * Они направленно связывают между собой разные узлы, перечисляемые в модели.
 *
 * Подмодель каждого поля реализуется где-то в другом файле.
 */

@Singleton
final class MNodes @Inject() (
                               injector: Injector,
                             )
  extends EsModelStatic
  with EsmV2Deserializer
  with MacroLogsImpl
  with EsModelJsonWrites
  with EsDynSearchStatic[MNodeSearch]
  with EsModelStaticCacheableT
{

  private def esModel = injector.instanceOf[EsModel]
  implicit private def sioNotifierClient = injector.instanceOf[ISioNotifierStaticClient]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  // cache
  override def EXPIRE = 60.seconds
  override def CACHE_KEY_SUFFIX = ".nc"

  override type T = MNode

  override def ES_TYPE_NAME = "n2"
  override def ES_INDEX_NAME = MainEsIndex.getMainIndexAliasName()

  @inline def Fields = MNodeFields

  /** Почти-собранный play.json.Format. */
  private val DATA_FORMAT: OFormat[MNode] = {
    val F = Fields
    (
      (__ \ F.Common.COMMON_FN).format[MNodeCommon] and
      (__ \ F.Meta.META_FN).format[MMeta] and
      (__ \ F.Extras.EXTRAS_FN).formatNullable[MNodeExtras]
        .inmap [MNodeExtras] (
          opt2ImplMEmptyF( MNodeExtras ),
          implEmpty2OptF
        ) and
      (__ \ F.Edges.EDGES_FN).formatNullable[MNodeEdges]
        .inmap [MNodeEdges] (
          opt2ImplMEmptyF( MNodeEdges ),
          implEmpty2OptF
        ) and
      (__ \ F.Billing.BILLING_FN).formatNullable[MNodeBilling]
        .inmap [MNodeBilling] (
          opt2ImplMEmptyF( MNodeBilling ),
          implEmpty2OptF
        )
    )(
      {(common, meta, extras, edges, billing) =>
        MNode(common, meta, extras, edges, billing = billing)
      },
      {mnode =>
        import mnode._
        (common, meta, extras, edges, billing)
      }
    )
  }


  override protected def esDocReads(dmeta: EsDocMeta): Reads[MNode] = {
    DATA_FORMAT
      .map { withDocMeta(_, dmeta) }
  }

  /** Сериализация в JSON. */
  override def esDocWrites = DATA_FORMAT


  /** Собрать инстанс юзера на основе compat-API модели MPerson.apply(). */
  def applyPerson(lang: String, id: Option[String] = None, nameOpt: Option[String] = None,
                  mpm: MPersonMeta = MPersonMeta.empty): MNode = {
    MNode(
      id = id,
      common = MNodeCommon(
        ntype       = MNodeTypes.Person,
        isDependent = false
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt = nameOpt,
          langs   = lang :: Nil,
        ),
        person  = mpm
      )
    )
  }


  /**
   * Сбор статистики по кол-ву N2-узлов различных типов.
   *
   * @param dsa Критерий выборки, если требуется.
   * @return Карта, где ключ -- тип узла, а значение -- кол-во результатов в индексе.
   */
  def ntypeStats(dsa: MNodeSearch = null): Future[Map[MNodeType, Long]] = {
    val _esModel = esModel
    import _esModel.api._

    val aggName = "ntypeAgg"
    Option( dsa )
      .fold( this.prepareSearch() )( this.prepareSearch1 )
      .addAggregation(
        AggregationBuilders
          .terms( aggName )
          .field( MNodeFields.Common.NODE_TYPE_FN )
          .size( MNodeTypes.values.length )
      )
      .setSize(0)
      .executeFut()
      .map { resp =>
        resp
          .getAggregations
          .get[Terms]( aggName )
          .getBuckets
          .iterator()
          .asScala
          .map { bucket =>
            val ntype = MNodeTypes.withValue( bucket.getKeyAsString )
            ntype -> bucket.getDocCount
          }
          .toMap
      }
  }


  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    IndexMapping(
      source = Some( FSource(enabled = someTrue) ),
      properties = Some {
        val F = Fields
        List[(String, IEsMappingProps)](
          F.Common.COMMON_FN     -> MNodeCommon,
          F.Meta.META_FN         -> MMeta,
          F.Extras.EXTRAS_FN     -> MNodeExtras,
          F.Edges.EDGES_FN       -> MNodeEdges,
          F.Billing.BILLING_FN   -> MNodeBilling,
        )
          .esSubModelsJsObjects( nested = false )
      }
    )
  }


  override def _deleteById(id: String)(fut: Future[Boolean]): Future[Boolean] = {
    val delFut = super._deleteById(id)(fut)
    for (isDeleted <- delFut) {
      _afterDelete(id, isDeleted)
    }
    delFut
  }

  private def _afterDelete(id: String, isDeleted: Boolean = true): Unit = {
    val evt = MNodeDeleted(id, isDeleted)
    sioNotifierClient.publish(evt)
  }

  override def _deleteByIds(ids: Iterable[String])(fut: Future[Option[BulkResponse]]): Future[Option[BulkResponse]] = {
    val delFut = super._deleteByIds(ids)(fut)
    for (delOpt <- delFut if delOpt.nonEmpty)
      for (id <- ids)
        _afterDelete(id)
    delFut
  }

  /**
   * Сохранить экземпляр в хранилище модели.
   * При успехе будет отправлено событие [[io.suggest.n2.node.event.MNodeSaved]] в шину событий.
   *
   * @return Фьючерс с новым/текущим id.
   */
  override def _save(m: MNode)(f: () => Future[EsDocMeta]): Future[EsDocMeta] = {
    // Запретить сохранять узел без id, если его тип не подразумевает генерацию рандомных id.
    if (m.id.isEmpty && !m.common.ntype.randomIdAllowed) {
      throw new IllegalArgumentException(s"id == None, but node type [${m.common.ntype}] does NOT allow random ids.")
    } else {
      val saveFut = super._save(m)(f)
      for (docMeta <- saveFut) {
        val mnode2 = withDocMeta( m, docMeta )
        val evt = MNodeSaved(mnode2, isCreated = m.id.isEmpty)
        sioNotifierClient.publish(evt)
      }
      saveFut
    }
  }

  override def withDocMeta(m: MNode, docMeta: EsDocMeta): MNode = {
    m.copy(
      id = docMeta.id,
      versioning = docMeta.version,
    )
  }

}


/** Класс-реализация модели узла графа N2. */
final case class MNode(
                        common                      : MNodeCommon,
                        meta                        : MMeta           = MMeta(),
                        extras                      : MNodeExtras     = MNodeExtras.empty,
                        edges                       : MNodeEdges      = MNodeEdges.empty,
                        billing                     : MNodeBilling    = MNodeBilling.empty,
                        override val id             : Option[String]  = None,
                        override val versioning     : EsDocVersion    = EsDocVersion.empty,
                      )
  extends EsModelT
{

  lazy val guessDisplayName: Option[String] = {
    meta.basic
      .guessDisplayName
      .orElse { MNodeTypesJvm.guessNodeDisplayName(this) }
  }

  def guessDisplayNameOrId: Option[String] = {
    guessDisplayName
      .orElse { id }
  }

  def guessDisplayNameOrIdOrQuestions: String = {
    guessDisplayNameOrIdOr("???")
  }
  def guessDisplayNameOrIdOrEmpty: String = {
    guessDisplayNameOrIdOr("")
  }

  def guessDisplayNameOrIdOr(or: => String): String = {
    guessDisplayNameOrId.getOrElse(or)
  }


  /** Система быстрого доступа к рутинным операциям с полями класса. */
  object Quick {

    /** Быстрые операции для полей ADN-узла. */
    object Adn {

      /** Подготовить эджи для картинки из MAdnResView. */
      private def _jdIdWithEdge(f: MAdnResView => IterableOnce[MJdEdgeId]): LazyList[(MJdEdgeId, MEdge)] = {
        (for {
          adn     <- extras.adn.iterator
          jdId    <- f(adn.resView).iterator
          medge   <- edges.withUid( jdId.edgeUid ).out.iterator
        } yield {
          (jdId, medge)
        })
          .to( LazyList )
      }

      /** Эдж картинки-логотипа adn-узла. */
      lazy val logo = _jdIdWithEdge(
        MAdnResView.logo
          .andThen( Traversal.fromTraverse[Option, MJdEdgeId] )
          .getAll
      )
        .headOption

      /** Эдж картинки приветствия adn-узла. */
      lazy val wcFg = _jdIdWithEdge(
        MAdnResView.wcFg
          .andThen( Traversal.fromTraverse[Option, MJdEdgeId] )
          .getAll
      )
        .headOption

      /** Списочек галеры картинок adn-узла. */
      lazy val galImgs = _jdIdWithEdge(
        MAdnResView.galImgs.get
      )

    }

  }

}

object MNode {

  implicit class MNodeOptFutOps(val mnodeOptFut: Future[Option[MNode]]) extends AnyVal {

    /** Отфильтровать узел по типу. Создано для замены MNodesCache.getByIdType(). */
    def withNodeType(ntype: MNodeType)(implicit ec: ExecutionContext): Future[Option[MNode]] = {
      for (mnodeOpt <- mnodeOptFut) yield {
        mnodeOpt.filter { mnode =>
          mnode.common.ntype eqOrHasParent ntype
        }
      }
    }

  }


  def common  = GenLens[MNode](_.common)
  def meta    = GenLens[MNode](_.meta)
  def extras  = GenLens[MNode](_.extras)
  def edges   = GenLens[MNode](_.edges)
  def billing = GenLens[MNode](_.billing)
  def id      = GenLens[MNode](_.id)
  def versionOpt = GenLens[MNode](_.versioning)


  def node_meta_basic_dateEdited_RESET = {
    MNode.meta
      .andThen( MMeta.basic )
      .andThen( MBasicMeta.dateEdited )
      .replace( Some(OffsetDateTime.now()) )
  }

  /** Make calendar node. */
  def calendar(id: Option[String], calType: MCalType, name: String, data: String): MNode = {
    MNode(
      id = id,
      common = MNodeCommon(
        ntype       = MNodeTypes.Calendar,
        isDependent = false,
      ),
      extras = MNodeExtras(
        calendar = Some(
          MNodeCalendar(
            calType = calType,
            data    = data,
          )
        ),
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt = Option( name ),
        ),
      )
    )
  }

}


trait MNodesJmxMBean extends EsModelJMXMBeanI
final class MNodesJmx @Inject() (
                                  override val injector: Injector,
                                )
  extends EsModelJMXBaseImpl
    with MNodesJmxMBean
{

  override def companion = injector.instanceOf[MNodes]
  override type X = MNode

}
