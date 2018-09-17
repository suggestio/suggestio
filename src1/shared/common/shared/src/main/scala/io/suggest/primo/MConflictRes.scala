package io.suggest.primo

import enumeratum._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 22:03
  * Description: Модель описания действия для разруливания конфликтов.
  */

object MConflictRes extends Enum[MConflictAction] {

  /** Взять новый, забыть старый. */
  case object ReplaceOld extends MConflictAction

  /** Объединить оба варианта. */
  case object Merge extends MConflictAction

  /** Оставить старый, забыть новый. */
  case object IgnoreNew extends MConflictAction


  override def values = findValues

}


/** Класс вариантов модели разрешения конфликтов. */
sealed abstract class MConflictAction extends EnumEntry
