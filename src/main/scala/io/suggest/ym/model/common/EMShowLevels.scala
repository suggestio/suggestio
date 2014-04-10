package io.suggest.ym.model.common

import io.suggest.ym.model.AdShowLevel
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
  val deserializeShowLevels: PartialFunction[Any, Set[AdShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.map { rawSL => AdShowLevels.withName(rawSL.toString) }.toSet

    case s: String =>
      Set(AdShowLevels.withName(s))

    case null => Set.empty
  }
}

// Трейты уже не использовались, но требовали поддержки. Поэтому были удалены в ревизии, следующей за 5a533c360007.

