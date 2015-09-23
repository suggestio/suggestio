package io.suggest.model.n2.node

import io.suggest.model._
import io.suggest.model.n2.node.common.{MNodeCommon, EMNodeCommonStatic}
import io.suggest.model.n2.tag.{MNodeTagInfo, MNodeTagInfoMappingT}
import io.suggest.model.search.EsDynSearchStatic
import io.suggest.util.SioEsUtil._
import io.suggest.util.{MacroLogsImpl, SioConstants}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Map

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

  override protected def esDocReads(meta: IEsDocMeta): Reads[MNode] = (
    __.read[MNodeCommon] and
    __.read[MNodeTagInfo]
  ) { (common, mntag) =>
    MNode(common, mntag, meta.id, meta.version)
  }

  /** Сериализация в JSON. */
  override val esDocWrites: Writes[MNode] = (
    __.write[MNodeCommon] and
    __.write[MNodeTagInfo]
  ) { mnode =>
    (mnode.common, mnode.tag)
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  common                      : MNodeCommon,
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
