package io.suggest.model.n2

import io.suggest.model._
import io.suggest.model.n2.tag.vertex.{EMTagVertex, MTagVertex, EMTagVertexT, EMTagEdgeStaticT}
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
 * Суть модели: Узлы -- это точки-сущности графа, но они как бы полиморфны извнутри.
 * Т.е. Узлы имеют вертексы (vertex), которые являются как бы свойствами, которых может и не быть.
 * Т.е. например Узел _asdfa243fa23faw89fe -- это просто тег, т.о. он имеет соотв.значение в поле tagVertex,
 * но остальные vertex-поля пустые.
 *
 * Есть также ребра, т.е. в Узле-карточке перечислены теги, по которым карточка будет искаться.
 * Там лежат по сути внутри _id других Узлов этой модели.
 *
 * Подмодель каждого вертекса реализуется где-то в другом файле.
 * Модель является началом реализации архитектуры N2 проекта SiO2.
 */
object MNode extends EsModelStaticT with EsmV2Deserializer with MacroLogsImpl with EMTagEdgeStaticT with IEsDocJsonWrites {

  override type T = MNode
  override val ES_TYPE_NAME: String = "n2"

  @deprecated("Delete it, use deserializeOne2() instead.", "2015.sep.11")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MNode = {
    throw new UnsupportedOperationException("Deprecated API NOT IMPLEMENTED.")
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = true),
      FieldSource(enabled = true)
    )
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[MNode] = {
    EMTagVertex.READS
      .map { tagVertexOpt =>
        MNode(tagVertexOpt, meta.id, meta.version)
      }
  }

  /** Сериализация в JSON. */
  override val esDocWrites: Writes[MNode] = {
    EMTagVertex.WRITES
      .contramap[MNode](_.tagVertex)
  }

}


/** Класс-реализация модели узла графа N2. */
case class MNode(
  override val tagVertex     : Option[MTagVertex] = None,
  override val id            : Option[String] = None,
  override val versionOpt    : Option[Long]   = None
)
  extends EsModelT
  with EsModelJsonWrites
  with EMTagVertexT
{

  override type T = MNode
  override def companion = MNode

}
