package io.suggest.ym.model.tag

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 18:22
 * Description: ES-модель всех тегов в системе. Она используется для поиска тегов.
 * _id каждого тега -- это текстовое название этого тега.
 *
 * Поля содержат индексируемую инфу для поиска: географию тега, популярность в карточках и т.д.
 */
object MTag extends EsModelStaticT with EsmV2Deserializer with MacroLogsImpl {

  override type T = MTag

  override val ES_TYPE_NAME = "tag"

  /** Название ES-поля с названием тега. Оно же перекидывается в _id. */
  val NAME_FN = "n"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldId(path = NAME_FN),
      FieldAll(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true)
    )
  }

  /** Внутренний JSON Reads. В ходе расширения, он будет использовать functional syntax. */
  private val _reads = {
    (__ \ NAME_FN).read[String]
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[MTag] = {
    // Пока поле только одно, маппинг идёт через map(fun). Когда будет Reads#CBF, то нужно apply(fun).
    _reads.map { name =>
      MTag(
        name = name,
        versionOpt = meta.version
      )
    }
  }

  @deprecated("Not implemented, delete it and use deserializeOne2() instead.", "2015.sep.10")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MTag = {
    throw new UnsupportedOperationException("Deprecated API is not implemented. Use deserializeOne2() instead.")
  }

}


import MTag._


case class MTag(
  name        : String,
  versionOpt  : Option[Long]    = None
)
  extends EsModelPlayJsonT
  with EsModelT
{

  override type T = MTag
  override def id = Some(name)
  override def companion = MTag

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    NAME_FN -> JsString(name) ::
      acc
  }

}
