package io.suggest.model.n2.node

import io.suggest.adn.edit.m.MAdnResView
import javax.inject.{Inject, Singleton}
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.bill.MNodeBilling
import io.suggest.model.n2.edge.{MEdge, MNodeEdges}
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.event.{MNodeDeleted, MNodeSaved}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.es.util.SioEsUtil._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model._
import io.suggest.es.search.EsDynSearchStatic
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.jd.MJdEdgeId
import io.suggest.util.logs.MacroLogsImpl
import monocle.Traversal
import monocle.macros.GenLens
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
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
 * В итоге получилась архитектура, похожая на модели zotonic: m_rsc + m_edge в одном флаконе.
 *
 * Есть также ребра (поле edges), описанные моделями [[io.suggest.model.n2.edge.MEdge]].
 * Они направленно связывают между собой разные узлы, перечисляемые в модели.
 *
 * Подмодель каждого поля реализуется где-то в другом файле.
 */

@Singleton
final class MNodes @Inject() (
                               esModel    : EsModel
                             )(implicit
                               ec         : ExecutionContext,
                               sn         : SioNotifierStaticClientI,
                             )
  extends EsModelStatic
  with EsmV2Deserializer
  with MacroLogsImpl
  with IGenEsMappingProps
  with EsModelJsonWrites
  with EsDynSearchStatic[MNodeSearch]
  with EsModelStaticCacheableT
{

  // cache
  override val EXPIRE = 60.seconds
  override val CACHE_KEY_SUFFIX = ".nc"

  override type T = MNode
  override def ES_TYPE_NAME = MNodeFields.ES_TYPE_NAME

  def Fields = MNodeFields

  @deprecated("Delete it, use deserializeOne2() instead.", "2015.sep.11")
  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): MNode = {
    throw new UnsupportedOperationException("Deprecated API NOT IMPLEMENTED.")
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true)
    )
  }

  /** Почти-собранный play.json.Format. */
  val DATA_FORMAT: OFormat[MNode] = (
    (__ \ Fields.Common.COMMON_FN).format[MNodeCommon] and
    (__ \ Fields.Meta.META_FN).format[MMeta] and
    (__ \ Fields.Extras.EXTRAS_FN).formatNullable[MNodeExtras]
      .inmap [MNodeExtras] (
        opt2ImplMEmptyF( MNodeExtras ),
        implEmpty2OptF
      ) and
    (__ \ Fields.Edges.EDGES_FN).formatNullable[MNodeEdges]
      .inmap [MNodeEdges] (
        opt2ImplMEmptyF( MNodeEdges ),
        implEmpty2OptF
    ) and
    (__ \ Fields.Billing.BILLING_FN).formatNullable[MNodeBilling]
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

  override protected def esDocReads(dmeta: IEsDocMeta): Reads[MNode] = {
    DATA_FORMAT
      .map { _.withDocMeta(dmeta) }
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
          langs   = List(lang)
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
    import esModel.api._

    val aggName = "ntypeAgg"
    (if (dsa == null) this.prepareSearch() else this.prepareSearch1(dsa))
      .addAggregation(
        AggregationBuilders.terms(aggName)
          .field( MNodeFields.Common.NODE_TYPE_FN )
      )
      .executeFut()
      .map { resp =>
        resp.getAggregations
          .get[Terms](aggName)
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


  private def _obj(fn: String, model: IGenEsMappingProps): FieldObject = {
    FieldObject(fn, enabled = true, properties = model.generateMappingProps)
  }
  override def generateMappingProps: List[DocField] = {
    List(
      _obj(Fields.Common.COMMON_FN,   MNodeCommon),
      _obj(Fields.Meta.META_FN,       MMeta),
      _obj(Fields.Extras.EXTRAS_FN,   MNodeExtras),
      _obj(Fields.Edges.EDGES_FN,     MNodeEdges),
      _obj(Fields.Ad.AD_FN,           MNodeAd),
      _obj(Fields.Billing.BILLING_FN, MNodeBilling)
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
    sn.publish(evt)
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
   * При успехе будет отправлено событие [[io.suggest.model.n2.node.event.MNodeSaved]] в шину событий.
   *
   * @return Фьючерс с новым/текущим id.
   */
  override def _save(m: MNode)(f: () => Future[String]): Future[String] = {
    // Запретить сохранять узел без id, если его тип не подразумевает генерацию рандомных id.
    if (m.id.isEmpty && !m.common.ntype.randomIdAllowed) {
      throw new IllegalArgumentException(s"id == None, but node type [${m.common.ntype}] does NOT allow random ids.")
    } else {
      val saveFut = super._save(m)(f)
      for (adnId <- saveFut) {
        val mnode2 = m.copy(id = Option(adnId))
        val evt = MNodeSaved(mnode2, isCreated = m.id.isEmpty)
        sn.publish(evt)
      }
      saveFut
    }
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  common                      : MNodeCommon,
  meta                        : MMeta           = MMeta(),
  extras                      : MNodeExtras     = MNodeExtras.empty,
  edges                       : MNodeEdges      = MNodeEdges.empty,
  // TODO ad - Удалить: это остатки старого рендера, который зависит от непортированных на jd-ads кусков кода в www (рендер, MAdAi, etc)
  ad                          : MNodeAd         = MNodeAd.empty,
  billing                     : MNodeBilling    = MNodeBilling.empty,
  override val id             : Option[String]  = None,
  override val versionOpt     : Option[Long]    = None
)
  extends EsModelT
{

  def withDocMeta(dmeta: IEsDocMeta): MNode = {
    copy(
      id = dmeta.id,
      versionOpt = dmeta.version
    )
  }

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
      private def _jdIdWithEdge(f: MAdnResView => IterableOnce[MJdEdgeId]): Iterator[(MJdEdgeId, MEdge)] = {
        for {
          adn     <- extras.adn.iterator
          jdId    <- f(adn.resView)
          medge   <- edges.withUid( jdId.edgeUid ).out.iterator
        } yield {
          (jdId, medge)
        }
      }

      /** Эдж картинки-логотипа adn-узла. */
      lazy val logo = _jdIdWithEdge(
        (MAdnResView.logo composeTraversal Traversal.fromTraverse[Option, MJdEdgeId])
          .getAll
      )
        .buffered
        .headOption

      /** Эдж картинки приветствия adn-узла. */
      lazy val wcFg = _jdIdWithEdge(
        (MAdnResView.wcFg composeTraversal Traversal.fromTraverse[Option, MJdEdgeId])
          .getAll
      )
        .buffered
        .headOption

      /** Списочек галеры картинок adn-узла. */
      lazy val galImgs = _jdIdWithEdge(
        MAdnResView.galImgs.get
      )
        .to( LazyList )

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


  val common  = GenLens[MNode](_.common)
  val meta    = GenLens[MNode](_.meta)
  val extras  = GenLens[MNode](_.extras)
  val edges   = GenLens[MNode](_.edges)
  val billing = GenLens[MNode](_.billing)
  val id      = GenLens[MNode](_.id)
  val versionOpt = GenLens[MNode](_.versionOpt)

}


trait MNodesJmxMBean extends EsModelJMXMBeanI
final class MNodesJmx @Inject() (
                                  override val companion      : MNodes,
                                  override val esModelJmxDi   : EsModelJmxDi,
                                )
  extends EsModelJMXBaseImpl
    with MNodesJmxMBean
{
  override type X = MNode
}

/** Интерфейс для DI-поля, содержащего инстанс MNodes. */
trait IMNodes {
  val mNodes: MNodes
}
