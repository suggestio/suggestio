package io.suggest.model.n2.node

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.n2.FieldNamesL1
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.extra.{EMNodeExtras, MNodeExtras}
import io.suggest.model.n2.node.meta.{MPersonMeta, MNodeMeta}
import io.suggest.model.search.EsDynSearchStatic
import io.suggest.util.SioEsUtil._
import io.suggest.util.{MacroLogsImpl, SioConstants}
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
 * В итоге получилась архитектура, похожая на модели zotonic: m_rsc + m_edge.
 *
 * Суть модели: Узлы -- это точки-сущности графа, но они как бы полиморфны извнутри.
 * Т.е. Узлы (node) имеют вершины (vertex), которые являются как бы свойствами, которых может и не быть.
 * Т.е. например Узел _asdfa243fa23faw89fe -- это просто тег, т.о. он имеет соотв.значение в поле tag.vertex,
 * но остальные *vertex-поля пустые.
 *
 * Есть также ребра, описанные в модели [[io.suggest.model.n2.edge.MEdge]].
 * Они направленно связывают между собой разные узлы, перечисляемые в этой модели.
 *
 * Подмодель каждого поля/вертекса реализуется где-то в другом файле.
 * Модель является началом реализации архитектуры N2 проекта SiO2.
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

    object Common {
      def COMMON_FN = FieldNamesL1.Common.name
    }

    object Meta extends PrefixedFn {

      /** Имя поля на стороне ES, куда скидываются все метаданные. */
      def META_FN         = FieldNamesL1.Meta.name

      override protected def _PARENT_FN = META_FN

      def META_FLOOR_ESFN             = _fullFn( MNodeMeta.FLOOR_ESFN )
      def META_WELCOME_AD_ID_ESFN     = _fullFn( MNodeMeta.WELCOME_AD_ID )
      def META_NAME_ESFN              = _fullFn( MNodeMeta.NAME_ESFN )
      def META_NAME_SHORT_ESFN        = _fullFn( MNodeMeta.NAME_SHORT_ESFN )
      def META_NAME_SHORT_NOTOK_ESFN  = _fullFn( MNodeMeta.NAME_SHORT_NOTOK_ESFN )

    }

    object Extras extends PrefixedFn {

      def EXTRAS_FN  = FieldNamesL1.Extras.name
      override protected def _PARENT_FN = EXTRAS_FN
    }

    object Edges extends PrefixedFn {

      def EDGES_FN = FieldNamesL1.Edges.name
      override protected def _PARENT_FN = EDGES_FN

      /** Адрес nested-объектов, хранящих данные по эджам. */
      def EDGES_OUT_FULL_FN = _fullFn( MNodeEdges.OUT_FN )

      def EDGE_OUT_PREDICATE_FULL_FN = _fullFn( MNodeEdges.OUT_PREDICATE_FN )
      def EDGE_OUT_NODE_ID_FULL_FN   = _fullFn( MNodeEdges.OUT_NODE_ID_FN )
      def EDGE_OUT_ORDER_FULL_FN     = _fullFn( MNodeEdges.OUT_ORDER_FN )

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

  /** Почти-собранный play.json.Format. */
  val DATA_FORMAT: OFormat[MNode] = (
    (__ \ Fields.Common.COMMON_FN).formatNullable(MNodeCommon.FORMAT)
      .inmap[MNodeCommon](
        { _ getOrElse MNodeCommon(MNodeTypes.Tag, isDependent = true) },
        { Some.apply }
      ) and
    (__ \ Fields.Meta.META_FN).formatNullable[MNodeMeta]
      .inmap[MNodeMeta](
        _ getOrElse MNodeMeta.empty,
        Some.apply
      ) and
    // TODO EMNodeExtras нетривиальный READS внутри FORMAT из-за compatibility, он пока вынесен в отдельный файл.
    // Нужно будет почистить и заинлайнить FORMAT, когда в ES значения тегов переместяться на уровень extas из L1 (после JMX MNode.resaveMany() на продакшене).
    __.format(EMNodeExtras.COMPAT_FORMAT) and
    (__ \ Fields.Edges.EDGES_FN).formatNullable[MNodeEdges]
      .inmap [MNodeEdges] (
        _ getOrElse MNodeEdges.empty,
        { mne => if (mne.nonEmpty) Some(mne) else None }
      )
  )(
    {(common, nmeta, mntag, edges) =>
      MNode(common, nmeta, mntag, edges)
    },
    {mnode =>
      (mnode.common, mnode.meta, mnode.extras, mnode.edges)
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
      meta = MNodeMeta(
        nameOpt = nameOpt,
        langs   = List(lang),
        person  = mpm
      )
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(Fields.Common.COMMON_FN, enabled = true, properties = MNodeCommon.generateMappingProps),
      FieldObject(Fields.Meta.META_FN, enabled = true, properties = MNodeMeta.generateMappingProps),
      FieldObject(Fields.Extras.EXTRAS_FN, enabled = true, properties = MNodeExtras.generateMappingProps),
      FieldObject(Fields.Edges.EDGES_FN, enabled = true, properties = MNodeEdges.generateMappingProps)
    )
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  common                      : MNodeCommon,
  meta                        : MNodeMeta       = MNodeMeta.empty,
  extras                      : MNodeExtras     = MNodeExtras.empty,
  edges                       : MNodeEdges      = MNodeEdges.empty,
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
