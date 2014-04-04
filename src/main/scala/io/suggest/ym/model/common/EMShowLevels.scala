package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.ym.model.{AdShowLevels, AdShowLevel}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 18:23
 * Description: Поле showLevels для списка уровней отображения.
 */
object EMShowLevels {
  val SHOW_LEVELS_ESFN = "showLevels"

  /** Десериализатор списка уровней отображения. */
  val deserializeShowLevels: PartialFunction[AnyRef, Set[AdShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.map { rawSL => AdShowLevels.withName(rawSL.toString) }.toSet

    case s: String =>
      Set(AdShowLevels.withName(s))

    case null => Set.empty
  }
}

import EMShowLevels._


trait EMShowLevelsStatic[T <: EMShowLevels[T]] extends EsModelStaticT[T] {
  def generateMappingProps: List[DocField] = {
    FieldString(SHOW_LEVELS_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false) ::
    super.generateMappingProps
  }

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (SHOW_LEVELS_ESFN, value) =>
        acc.showLevels = deserializeShowLevels(value)
    }
  }
}

trait EMShowLevels[T <: EMShowLevels[T]] extends EsModelT[T] {

  var showLevels: Set[AdShowLevel]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (!showLevels.isEmpty) {
      acc.startArray(SHOW_LEVELS_ESFN)
      showLevels.foreach { sl =>
        acc.value(sl.toString)
      }
      acc.endArray()
    }
  }
}
