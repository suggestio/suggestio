package io.suggest.model.n2.tag.edge

import java.{util => ju}

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelCommonStaticT, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.09.15 16:30
 * Description: Поле тегов для узлов.
 * Десериализованное поле -- это карта, а сериализуется оно в JsArray значений исходной карты.
 */
object EMTagsEdge {

  /** Название поля тегов верхнего уровня. */
  val TAGS_FN = "tag"

}


import io.suggest.model.n2.tag.edge.EMTagsEdge._


/** Базовый аддон для компаньонов моделей. */
// TODO Убрать sealed когда здесь будет уже нормальная десериализация для immutable-моделей через play.json.
sealed trait EMTagsEdgeStatic extends EsModelCommonStaticT {

  override type T <: EMTagsEdge

  abstract override def generateMappingProps: List[DocField] = {
    val field = FieldNestedObject(
      id          = TAGS_FN,
      enabled     = true,
      properties  = MTagEdge.generateMappingProps
    )
    field :: super.generateMappingProps
  }

}
/** Аддон для компаньонов моделей с legacy-десериализацией полей. */
trait EMTagsEdgeStaticMut extends EMTagsEdgeStatic with EsModelStaticMutAkvT {

  override type T <: EMTagsEdgeMut

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (TAGS_FN, values: ju.List[_]) =>
        import scala.collection.JavaConversions._
        acc.tags = values
          .iterator()
          .map { raw =>
            val tag = MTagEdge.fromJackson( raw )
            tag.id -> tag
          }
          .toMap
    }
  }

}


/** Интерфейс для доступа к полю. */
trait ITags {
  /** Текущая карта тегов. */
  def tags: TagsMap_t
}

/** Аддон для произвольных реализация моделей. */
trait EMTagsEdge extends EsModelPlayJsonT with ITags {

  override type T <: EMTagsEdge

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
trait EMTagsEdgeMut extends EMTagsEdge {
  override type T <: EMTagsEdgeMut

  var tags: TagsMap_t
}
