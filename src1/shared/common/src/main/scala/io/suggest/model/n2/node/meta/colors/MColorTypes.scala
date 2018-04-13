package io.suggest.model.n2.node.meta.colors

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.18 11:11
  * Description: Модель типов цветов.
  */
object MColorTypes extends StringEnum[MColorType] {

  /** Цвет фона. */
  case object Bg extends MColorType("b")

  /** Цвет контента. */
  case object Fg extends MColorType("f")


  override def values = findValues

}


sealed abstract class MColorType(override val value: String) extends StringEnumEntry

object MColorType {

  implicit def mColorTypeFormat: Format[MColorType] = {
    EnumeratumUtil.valueEnumEntryFormat( MColorTypes )
  }

  implicit def univEq: UnivEq[MColorType] = UnivEq.derive

}