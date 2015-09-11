package io.suggest.model.n2

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil.{Field, DocField}
import play.api.libs.json.Reads

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 14:04
 * Description: Модель "Узла" с большой буквы. Появилась в ходе решения объеденить
 * ADN-узлы, карточки, теги, юзеры и прочее в единую модель узлов графа.
 * Суть модели: "узлы" графа имеют "свойства".
 * На момент создания единственным доступным свойством было "tag".
 * Свойство -- это по сути подмодель, живущая внутри отдельного ВСЕГДА опционального поля.
 * {{{
 *  tag: Option[MTag] = None
 * }}}
 * Подмодель свойства реализуется где-то в другом файле.
 *
 * Модель является началом реализации архитектуры N2 проекта SiO2.
 */
object MNode extends EsModelStaticT with EsmV2Deserializer with MacroLogsImpl {

  override type T = MNode

  override def ES_TYPE_NAME: String = ???

  override protected def esDocReads(meta: IEsDocMeta): Reads[MNode] = ???

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MNode = ???

  override def generateMappingStaticFields: List[Field] = ???

  override def generateMappingProps: List[DocField] = ???

}


case class MNode(
  override val id            : Option[String] = None,
  override val versionOpt    : Option[Long]   = None
)
  extends EsModelPlayJsonT
  with EsModelT
{

  override type T = MNode
  override def companion = MNode

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    acc     // TODO
  }

}
