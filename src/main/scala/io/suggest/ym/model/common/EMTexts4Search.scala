package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import java.{util => ju, lang => jl}
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.14 10:30
 * Description: Тексты из других моделей, которые необходимы для улучшения поиска (индексации) контента
 * складируются через эту модель.
 */
object EMTexts4Search {

  /** root-object поле, в котором лежит внутренний объект с полями для индексации. */
  val SEARCH_TEXT_ESFN = "searchText"

}

import EMTexts4Search._

trait EMText4SearchStatic[T <: EMTexts4Search[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(SEARCH_TEXT_ESFN, enabled = true, properties = Texts4Search.generateMappingProps) ::
      super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (SEARCH_TEXT_ESFN, value: ju.Map[_,_]) =>
        acc.texts4search = Texts4Search.deserialize(value)
    }
  }
}

trait EMTexts4Search[T <: EMTexts4Search[T]] extends EsModelT[T] {
  var texts4search: Texts4Search

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (!texts4search.isEmpty) {
      acc.startObject(SEARCH_TEXT_ESFN)
      texts4search.writeFields(acc)
      acc.endObject()
    }
  }
}


object Texts4Search {

  /** Название поля, которое содержит строковое название категории. */
  val USER_CAT_ESFN = "userCat"

  /** Генерация пропертисов объекта. */
  def generateMappingProps = List(
    FieldString(USER_CAT_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true, boost = Some(0.5F))
  )

  /** Десериализация ранее сериализованного Texts4Search. */
  def deserialize(value: ju.Map[_,_], acc0: Texts4Search = Texts4Search()): Texts4Search = {
    if (value != null) {
      Option(value.get(USER_CAT_ESFN)).map {
        case catsRaw: jl.Iterable[_] =>
          acc0.userCat = catsRaw.foldLeft[List[String]] (Nil) {
            (acc, e) => e.toString :: acc
          }.reverse
      }
    }
    acc0
  }
  
}

import Texts4Search._

/**
 * Класс с текстовыми полями, подлежащими полнотекстовой индексации.
 * @param userCat Список из названий категорий.
 */
case class Texts4Search(
  var userCat: List[String] = Nil
) {
  @JsonIgnore
  def isEmpty = userCat.isEmpty

  def writeFields(acc: XContentBuilder) = {
    if (!userCat.isEmpty)
      acc.array(USER_CAT_ESFN, userCat: _*)
  }
}
