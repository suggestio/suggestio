package io.suggest.model.n2.node

import com.google.inject.{Inject, Singleton}
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.bill.MNodeBilling
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.event.{MNodeDeleted, MNodeSaved}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.es.util.SioEsUtil._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model._
import io.suggest.es.search.EsDynSearchStatic
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

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
  override val mCommonDi: MEsModelDiVal
)
  extends EsModelStatic
  with EsmV2Deserializer
  with MacroLogsImpl
  with IGenEsMappingProps
  with EsModelJsonWrites
  with EsDynSearchStatic[MNodeSearch]
{
  import mCommonDi._

  override type T = MNode
  override def ES_TYPE_NAME = MNodeFields.ES_TYPE_NAME

  def Fields = MNodeFields

  @deprecated("Delete it, use deserializeOne2() instead.", "2015.sep.11")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MNode = {
    throw new UnsupportedOperationException("Deprecated API NOT IMPLEMENTED.")
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      /*FieldAll(
        enabled = true,
        analyzer        = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN
      ),*/
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
    (__ \ Fields.Geo.GEO_FN).formatNullable[MNodeGeo]
      .inmap [MNodeGeo] (
        opt2ImplMEmptyF( MNodeGeo ),
        implEmpty2OptF
      ) and
    (__ \ Fields.Ad.AD_FN).formatNullable[MNodeAd]
      .inmap [MNodeAd] (
        opt2ImplMEmptyF( MNodeAd ),
        implEmpty2OptF
      ) and
    (__ \ Fields.Billing.BILLING_FN).formatNullable[MNodeBilling]
      .inmap [MNodeBilling] (
        opt2ImplMEmptyF( MNodeBilling ),
        implEmpty2OptF
      )
  )(
    {(common, meta, extras, edges, geo, ad, billing) =>
      MNode(common, meta, extras, edges, geo, ad, billing)
    },
    {mnode =>
      import mnode._
      (common, meta, extras, edges, geo, ad, billing)
    }
  )

  override protected def esDocReads(dmeta: IEsDocMeta): Reads[MNode] = {
    DATA_FORMAT
      .map { _.withDocMeta(dmeta) }
  }

  /** Сериализация в JSON. */
  override def esDocWrites = DATA_FORMAT


  /** Враппер над getById(), осуществляющий ещё и фильтрацию по типу узла. */
  def getByIdType(id: String, ntype: MNodeType): Future[Option[T]] = {
    // TODO Opt фильтрация идёт client-side. Надо бы сделать server-side БЕЗ серьезных потерь производительности и реалтайма.
    getById(id).map {
      _.filter {
        _.common.ntype eqOrHasParent ntype
      }
    }
  }


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
  def ntypeStats(dsa: MNodeSearch = new MNodeSearchDfltImpl): Future[Map[MNodeType, Long]] = {
    val aggName = "ntypeAgg"
    prepareSearch(dsa)
      .addAggregation(
        AggregationBuilders.terms(aggName)
          .field( MNodeFields.Common.NODE_TYPE_FN )
      )
      .execute()
      .map { resp =>
        resp.getAggregations
          .get[Terms](aggName)
          .getBuckets
          .iterator()
          .map { bucket =>
            val ntype: MNodeType = MNodeTypes.withName( bucket.getKeyAsString )
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
      _obj(Fields.Geo.GEO_FN,         MNodeGeo),
      _obj(Fields.Ad.AD_FN,           MNodeAd),
      _obj(Fields.Billing.BILLING_FN, MNodeBilling)
    )
  }

  /**
   * Удалить документ по id.
   *
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String): Future[Boolean] = {
    val delFut = super.deleteById(id)
    delFut onSuccess { case isDeleted =>
      val evt = MNodeDeleted(id, isDeleted)
      sn.publish(evt)
    }
    delFut
  }

  /**
   * Сохранить экземпляр в хранилище модели.
   * При успехе будет отправлено событие [[io.suggest.model.n2.node.event.MNodeSaved]] в шину событий.
   *
   * @return Фьючерс с новым/текущим id.
   */
  override def save(m: T): Future[String] = {
    // Запретить сохранять узел без id, если его тип не подразумевает генерацию рандомных id.
    if (m.id.isEmpty && !m.common.ntype.randomIdAllowed) {
      throw new IllegalArgumentException(s"id == None, but node type [${m.common.ntype}] does NOT allow random ids.")

    } else {
      val saveFut = super.save(m)
      saveFut.onSuccess { case adnId =>
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
  meta                        : MMeta,
  extras                      : MNodeExtras     = MNodeExtras.empty,
  edges                       : MNodeEdges      = MNodeEdges.empty,
  geo                         : MNodeGeo        = MNodeGeo.empty,
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
      .orElse { common.ntype.guessNodeDisplayName(this) }
  }

  def guessDisplayNameOrId: Option[String] = {
    guessDisplayName
      .orElse { id }
  }

  def guessDisplayNameOrIdOrEmpty: String = {
    guessDisplayNameOrId.getOrElse("")
  }

  def withEdges(edges2: MNodeEdges) = copy(edges = edges2)

  def withId(idOpt: Option[String]) = copy(id = idOpt)

}


trait MNodesJmxMBean extends EsModelJMXMBeanI
final class MNodesJmx @Inject() (
  override val companion  : MNodes,
  override val ec         : ExecutionContext
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
