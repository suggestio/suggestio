package io.suggest.ym.model.tag

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelCommonStaticT, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import play.api.libs.json.Json
import java.{util => ju}

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

}


import io.suggest.ym.model.tag.EMTags._


/** Базовый аддон для компаньонов моделей. */
// TODO Убрать sealed когда здесь будет уже нормальная десериализация для immutable-моделей через play.json.
sealed trait EMTagsStatic extends EsModelCommonStaticT {

  override type T <: EMTags

  abstract override def generateMappingProps: List[DocField] = {
    val field = FieldNestedObject(
      id          = TAGS_FN,
      enabled     = true,
      properties  = MNodeTag.generateMappingProps
    )
    field :: super.generateMappingProps
  }

}
/** Аддон для компаньонов моделей с legacy-десериализацией полей. */
trait EMTagsStaticMut extends EMTagsStatic with EsModelStaticMutAkvT {

  override type T <: EMTagsMut

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (TAGS_FN, values: ju.List[_]) =>
        import scala.collection.JavaConversions._
        acc.tags = values
          .iterator()
          .map { rawMap =>
            val m = rawMap.asInstanceOf[ ju.Map[String, String] ]
            val tagId = m.get(MNodeTag.ID_FN)
            val tag = MNodeTag(id = tagId)
            tagId -> tag
          }
          .toMap
    }
  }

}


/** Аддон для произвольных реализация моделей. */
trait EMTags extends EsModelPlayJsonT {

  override type T <: EMTags

  /** Текущая карта тегов. */
  def tags: Map[String, MNodeTag]

  abstract override def writeJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc0)
    val _tags = tags
    if (_tags.isEmpty) {
      acc1
    } else {
      TAGS_FN -> Json.toJson(tags.values) :: acc1
    }
  }
}
/** Аддон для mutable-моделей. */
trait EMTagsMut extends EMTags {
  override type T <: EMTagsMut

  var tags: Map[String, MNodeTag]
}
