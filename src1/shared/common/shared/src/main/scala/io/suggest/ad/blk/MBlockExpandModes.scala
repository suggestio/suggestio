package io.suggest.ad.blk

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.08.2019 23:19
  * Description: wide-флаг доэволюционировал до группы допустимых режимов,
  * и тут модель для этих значений.
  */
object MBlockExpandModes extends StringEnum[MBlockExpandMode] {

  /** Растягивание фона блока по ширине экрана без изменения высоты.
    * Содержимое блока центруется в получившейся полоске. */
  case object Wide extends MBlockExpandMode("w")

  /** Растягивание и фона, и содержимого по горизонтали
    * с доп.растяжкой по вертикали (в зависимости от размеров блока). */
  case object Full extends MBlockExpandMode("f")


  override def values = findValues

}


sealed abstract class MBlockExpandMode(override val value: String ) extends StringEnumEntry

object MBlockExpandMode {

  implicit def blockRenderModeJson: Format[MBlockExpandMode] =
    EnumeratumUtil.valueEnumEntryFormat( MBlockExpandModes )

  @inline implicit def univEq: UnivEq[MBlockExpandMode] = UnivEq.derive

  implicit class MBlockExpandModeExt( val mbem: MBlockExpandMode ) extends AnyVal {
    def msgCode: String =
      "_Block.expand." + mbem.value
  }

}
