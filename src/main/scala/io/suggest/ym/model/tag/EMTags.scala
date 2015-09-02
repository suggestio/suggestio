package io.suggest.ym.model.tag

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioConstants
import io.suggest.util.SioEsUtil._
import play.api.libs.json.{JsString, JsObject, JsValue}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.15 16:30
 * Description: Поле тегов для узлов.
 * Десериализованное поле -- это карта, а сериализуется оно в JsArray значений исходной карты.
 */
object EMTags {

  /** Название поля тегов верхнего уровня. */
  val TAGS_FN = "tag"

  /** Название поля с нормализованным именем тега. */
  def TAG_ID_FN = "n"

  /** Название под-поля тега c поддержкой полнотекстового поиска. */
  def FTS_SUBFN = SioConstants.SUBFIELD_FTS

}


import io.suggest.ym.model.tag.EMTags._


trait EMTagsStatic extends EsModelStaticMutAkvT {
  override type T <: EMTagsMut

  abstract override def generateMappingProps: List[DocField] = {
    val field = FieldNestedObject(id = TAGS_FN, enabled = true, properties = Seq(
      FieldString(
        id    = TAGS_FN,
        index = FieldIndexingVariants.analyzed,
        include_in_all = true,
        analyzer = SioConstants.TAG_AN,
        fields = Seq(
          FieldString(
            id        = FTS_SUBFN,
            index     = FieldIndexingVariants.analyzed,
            analyzer  = SioConstants.DFLT_AN,
            include_in_all = true
          )
        )
      )
    ))
    field :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (TAGS_FN, value) =>
        ???
    }
  }

}


trait EMTags extends EsModelPlayJsonT {
  override type T <: EMTags

  def tags: Map[String, MNodeTag]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    ???
  }
}


trait EMTagsMut extends EMTags {
  override type T <: EMTagsMut

  var tags: Map[String, MNodeTag]
}


/** Модель одного тега узла. */
case class MNodeTag(
  id: String
) {

  def toPlayJson: JsValue = {
    JsObject(Seq(
      TAG_ID_FN -> JsString(id)
    ))
  }

}
