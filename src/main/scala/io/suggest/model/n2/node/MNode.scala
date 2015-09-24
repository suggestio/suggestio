package io.suggest.model.n2.node

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.model.n2.node.common.{EMNodeCommon, MNodeCommon, EMNodeCommonStatic}
import io.suggest.model.n2.node.meta.{EMNodeMeta, MNodeMeta, EMNodeMetaStatic}
import io.suggest.model.n2.tag.{MNodeTagInfo, MNodeTagInfoMappingT}
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
  with MNodeTagInfoMappingT
  with EMNodeCommonStatic
  with EMNodeMetaStatic
  with IEsDocJsonWrites
  with EsDynSearchStatic[MNodeSearch]
{

  override type T = MNode
  override val ES_TYPE_NAME = "n2"

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

  /** Полусобранный десериализатор экземпляров модели. */
  private val _reads = {
    EMNodeCommon.READS and
    EMNodeMeta.FORMAT and
    __.read[MNodeTagInfo]
  }
  override protected def esDocReads(dmeta: IEsDocMeta): Reads[MNode] = {
    _reads { (common, nmeta, mntag) =>
      MNode(common, nmeta, mntag, dmeta.id, dmeta.version)
    }
  }

  /** Сериализация в JSON. */
  override val esDocWrites: Writes[MNode] = (
    EMNodeCommon.WRITES and
    EMNodeMeta.FORMAT and
    __.write[MNodeTagInfo]
  ) { mnode =>
    (mnode.common, mnode.meta, mnode.tag)
  }


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
  def applyPerson(lang: String, id: Option[String] = None): MNode = {
    MNode(
      id = id,
      common = MNodeCommon(
        ntype       = MNodeTypes.Person,
        isDependent = false
      ),
      meta = MNodeMeta(
        langs = List(lang)
      )
    )
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  common                      : MNodeCommon,
  meta                        : MNodeMeta       = MNodeMeta.empty,
  tag                         : MNodeTagInfo    = MNodeTagInfo.empty,
  override val id             : Option[String]  = None,
  override val versionOpt     : Option[Long]    = None
)
  extends EsModelT
  with EsModelJsonWrites
{

  override type T = MNode
  override def companion = MNode

}



trait MNodeJmxMBean extends EsModelJMXMBeanI
final class MNodeJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MNodeJmxMBean
{
  override def companion = MNode
}
