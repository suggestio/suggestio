package io.suggest.model.n2.node

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.n2.FieldNamesL1
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.extra.{EMNodeExtras, MNodeExtras}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.model.search.EsDynSearchStatic
import io.suggest.util.SioEsUtil._
import io.suggest.util.{MacroLogsImpl, SioConstants}
import io.suggest.ym.model.common.{EMAdnMMetadataStatic, MNodeMeta}
import org.elasticsearch.client.Client
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

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
 * Суть модели: Узлы -- это точки-сущности графа, но они как бы полиморфны извнутри.
 * Т.е. Узлы (node) имеют вершины (vertex), которые являются как бы свойствами, которых может и не быть.
 * Т.е. например Узел _asdfa243fa23faw89fe -- это просто тег, т.о. он имеет соотв.значение в поле tag.vertex,
 * но остальные *vertex-поля пустые.
 *
 * Есть также ребра (поле edges), описанные моделями [[io.suggest.model.n2.edge.MEdge]].
 * Они направленно связывают между собой разные узлы, перечисляемые в модели.
 *
 * Подмодель каждого поля реализуется где-то в другом файле.
 */
object MNode
  extends EsModelStaticT
  with EsmV2Deserializer
  with MacroLogsImpl
  with IGenEsMappingProps
  with IEsDocJsonWrites
  with EsDynSearchStatic[MNodeSearch]
{

  override type T = MNode
  override val ES_TYPE_NAME = "n2"

  /** Абсолютные названия полей наследуют иерархию модели. */
  object Fields {

    /** Абсолютные имена ES-полей в .common */
    object Common extends PrefixedFn {
      def COMMON_FN = FieldNamesL1.Common.name
      override protected def _PARENT_FN = COMMON_FN
      def NODE_TYPE_FN      = _fullFn( MNodeCommon.NODE_TYPE_FN )
      def IS_ENABLED_FN     = _fullFn( MNodeCommon.IS_ENABLED_FN )
      def IS_DEPENDENT_FN   = _fullFn( MNodeCommon.IS_DEPEND_FN )
    }

    /** Абсолютные имена ES-полей в .meta */
    object Meta extends PrefixedFn {
      /** Имя поля на стороне ES, куда скидываются все метаданные. */
      def META_FN                     = FieldNamesL1.Meta.name
      override protected def _PARENT_FN = META_FN

      def BASIC_NAME_SHORT_NOTOK_FN   = _fullFn( MMeta.Fields.Basic.NAME_SHORT_NOTOK_FN )
    }

    /** Абсолютные имена ES-полей в .extras */
    object Extras extends PrefixedFn {

      def EXTRAS_FN  = FieldNamesL1.Extras.name
      override protected def _PARENT_FN = EXTRAS_FN
      
      def ADN_IS_TEST_FN          = _fullFn( MNodeExtras.Fields.Adn.IS_TEST_FN )
      def ADN_SINKS_FN            = _fullFn( MNodeExtras.Fields.Adn.SINKS_FN )
      def ADN_RIGHTS_FN           = _fullFn( MNodeExtras.Fields.Adn.RIGHTS_FN )
      def ADN_SHOWN_TYPE_FN       = _fullFn( MNodeExtras.Fields.Adn.SHOWN_TYPE_FN )
      def ADN_SHOW_IN_SC_NL_FN    = _fullFn( MNodeExtras.Fields.Adn.SHOW_IN_SC_NL_FN )
    }

    /** Абсолютные имена ES-полей в .edges */
    object Edges extends PrefixedFn {

      def EDGES_FN = FieldNamesL1.Edges.name
      override protected def _PARENT_FN = EDGES_FN

      /** Адрес nested-объектов, хранящих данные по эджам. */
      def EDGES_OUT_FULL_FN = _fullFn( MNodeEdges.Fields.OUT_FN )

      import MNodeEdges.Fields.Out._

      def EDGE_OUT_PREDICATE_FULL_FN = _fullFn( OUT_PREDICATE_FN )
      def EDGE_OUT_NODE_ID_FULL_FN   = _fullFn( OUT_NODE_ID_FN )
      def EDGE_OUT_ORDER_FULL_FN     = _fullFn( OUT_ORDER_FN )
      def EDGE_OUT_INFO_SLS_FN       = _fullFn( OUT_INFO_SLS_FN )
    }

    /** Абсолютные названия географических полей.*/
    object Geo extends PrefixedFn {
      def GEO_FN = FieldNamesL1.Geo.name
      override protected def _PARENT_FN = GEO_FN

      def POINT_FN = _fullFn( MNodeGeo.Fields.POINT_FN )
    }

  }


  @deprecated("Delete it, use deserializeOne2() instead.", "2015.sep.11")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MNode = {
    throw new UnsupportedOperationException("Deprecated API NOT IMPLEMENTED.")
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(
        enabled = true,
        index_analyzer  = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN
      )
    )
  }

  /** JSON-форматирование поля meta сейчас реализовано через MMeta,
    * но ранее использовалась MNodeMeta. Этот груз совместимости с прошлым лежит тут. */
  val META_COMPAT_FORMAT: Format[MMeta] = {

    val META_FORMAT = (__ \ Fields.Meta.META_FN).format[MMeta]

    val META_COMPAT_READS: Reads[MMeta] = {
      META_FORMAT
        .orElse {
          (__ \ EMAdnMMetadataStatic.META_FN).read[MNodeMeta]
            .map { _.toMMeta }
        }
        .orElse {
          Reads[MMeta] { other =>
            LOGGER.warn("json metadata looks empty: " + other)
            JsSuccess( MMeta(MBasicMeta()) )
          }
        }
    }

    Format[MMeta](META_COMPAT_READS, META_FORMAT)
  }

  /** Почти-собранный play.json.Format. */
  val DATA_FORMAT: OFormat[MNode] = (
    (__ \ Fields.Common.COMMON_FN).formatNullable(MNodeCommon.FORMAT)
      .inmap[MNodeCommon](
        { _ getOrElse MNodeCommon(MNodeTypes.Tag, isDependent = true) },
        { Some.apply }
      ) and
    __.format(META_COMPAT_FORMAT) and
    // TODO EMNodeExtras нетривиальный READS внутри FORMAT из-за compatibility, он пока вынесен в отдельный файл.
    // Нужно будет почистить и заинлайнить FORMAT, когда в ES значения тегов переместяться на уровень extas из L1 (после JMX MNode.resaveMany() на продакшене).
    __.format(EMNodeExtras.COMPAT_FORMAT) and
    (__ \ Fields.Edges.EDGES_FN).formatNullable[MNodeEdges]
      .inmap [MNodeEdges] (
        _ getOrElse MNodeEdges.empty,
        { mne => if (mne.nonEmpty) Some(mne) else None }
      ) and
    (__ \ Fields.Geo.GEO_FN).formatNullable[MNodeGeo]
      .inmap [MNodeGeo] (
        _ getOrElse MNodeGeo.empty,
        { mng => if (mng.nonEmpty) Some(mng) else None }
      )
  )(
    {(common, meta, extras, edges, geo) =>
      MNode(common, meta, extras, edges, geo)
    },
    {mnode =>
      import mnode._
      (common, meta, extras, edges, geo)
    }
  )

  override protected def esDocReads(dmeta: IEsDocMeta): Reads[MNode] = {
    DATA_FORMAT
      .map { _.withDocMeta(dmeta) }
  }

  /** Сериализация в JSON. */
  override def esDocWrites = DATA_FORMAT


  /** Враппер над getById(), осуществляющий ещё и фильтрацию по типу узла. */
  def getByIdType(id: String, ntype: MNodeType)
                 (implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
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


  private def _obj(fn: String, model: IGenEsMappingProps): FieldObject = {
    FieldObject(fn, enabled = true, properties = model.generateMappingProps)
  }
  override def generateMappingProps: List[DocField] = {
    List(
      _obj(Fields.Common.COMMON_FN,   MNodeCommon),
      _obj(Fields.Meta.META_FN,       MMeta),
      _obj(Fields.Extras.EXTRAS_FN,   MNodeExtras),
      _obj(Fields.Edges.EDGES_FN,     MNodeEdges),
      _obj(Fields.Geo.GEO_FN,         MNodeGeo)
    )
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  common                      : MNodeCommon,
  meta                        : MMeta,
  extras                      : MNodeExtras     = MNodeExtras.empty,
  edges                       : MNodeEdges      = MNodeEdges.empty,
  geo                         : MNodeGeo        = MNodeGeo.empty,
  override val id             : Option[String]  = None,
  override val versionOpt     : Option[Long]    = None
)
  extends EsModelT
  with EsModelJsonWrites
{

  override type T = MNode
  override def companion = MNode

  def withDocMeta(dmeta: IEsDocMeta): T = {
    copy(
      id = dmeta.id,
      versionOpt = dmeta.version
    )
  }

}



trait MNodeJmxMBean extends EsModelJMXMBeanI
final class MNodeJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MNodeJmxMBean
{
  override def companion = MNode
}
