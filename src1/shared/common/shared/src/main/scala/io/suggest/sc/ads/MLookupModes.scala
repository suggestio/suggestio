package io.suggest.sc.ads

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.05.16 18:58
  * Description: Модель режимов поиска focused-карточек.
  * Используется для описания направления поиска относительно какой-то карточки: вокруг/до/после.
  *
  * sc focused v2+.
  */
object MLookupModes extends StringEnum[MLookupMode] {

  /** Режим поиска элементов вокруг запрашиваемой карточки вместе с карточкой. */
  case object Around extends MLookupMode("a") {
    override def toVisualString = "<->"
    override def withBefore = true
    override def withAfter  = true
  }

  /** Режим поиска предшествующих карточек. */
  case object Before extends MLookupMode("b") {
    override def toVisualString = "<<<"
    override def withBefore = true
    override def withAfter  = false
  }

  /** Режим поиска карточек после указанной. */
  case object After extends MLookupMode("c") {
    override def toVisualString = ">>>"
    override def withBefore = false
    override def withAfter  = true
  }


  override val values = findValues

}


/** Класс одного элемента модели [[MLookupModes]]. */
sealed abstract class MLookupMode(override val value: String) extends StringEnumEntry {

  def toVisualString: String

  /** Есть направление "назад"? */
  def withBefore  : Boolean

  /** Есть направление "вперёд"? */
  def withAfter   : Boolean

}


object MLookupMode {

  /** Поддержка play-json. */
  implicit def MLOOKUP_MODE_FORMAT: Format[MLookupMode] = {
    EnumeratumUtil.valueEnumEntryFormat( MLookupModes )
  }

  @inline implicit def univEq: UnivEq[MLookupMode] = UnivEq.derive

}

