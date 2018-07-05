package io.suggest.sc.sc3

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.09.16 11:28
  * Description: Кросс-платформенная модель типов sc-resp-экшенов.
  */

/** Кросс-платформенная модель типов экшенов sc-ответов. */
object MScRespActionTypes extends StringEnum[MScRespActionType] {

  /** Тип экшена с индексом выдачи. */
  case object Index extends MScRespActionType("i")

  /** Тип экшена с плиткой выдачи. */
  case object AdsTile extends MScRespActionType("t")

  /** Тип экшена с карточками focused-выдачи. */
  case object AdsFoc extends MScRespActionType("f")

  /** Экшен результата поиска узлов. */
  case object SearchNodes extends MScRespActionType("s")


  /** Список всех значений модели. */
  override val values = findValues

}


/** Класс одного элемента модели типов. */
sealed abstract class MScRespActionType(override val value: String)
  extends StringEnumEntry


/** Статическая поддержка элементов модели [[MScRespActionType]]. */
object MScRespActionType {

  implicit def MSC_RESP_ACTION_TYPE_FORMAT: Format[MScRespActionType] = {
    EnumeratumUtil.valueEnumEntryFormat( MScRespActionTypes )
  }

  implicit def univEq: UnivEq[MScRespActionType] = UnivEq.derive

}

