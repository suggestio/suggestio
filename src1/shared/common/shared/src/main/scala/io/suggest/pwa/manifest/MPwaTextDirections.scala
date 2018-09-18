package io.suggest.pwa.manifest

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:53
  * Description: Модель направлений текста.
  */
object MPwaTextDirections extends StringEnum[MPwaTextDirection] {

  case object Auto extends MPwaTextDirection("auto")

  case object Ltr extends MPwaTextDirection("ltr")

  case object Rtl extends MPwaTextDirection("rtl")


  override lazy val values = findValues

  final def default: MPwaTextDirection = Auto

}


/** Класс модели направления текста в приложении. */
sealed abstract class MPwaTextDirection(override val value: String) extends StringEnumEntry with Product {

  override final def toString = value

}


object MPwaTextDirection {

  @inline implicit def univEq: UnivEq[MPwaTextDirection] = UnivEq.derive

  implicit def MPWA_TEXT_DIRECTION_FORMAT: Format[MPwaTextDirection] = {
    EnumeratumUtil.valueEnumEntryFormat( MPwaTextDirections )
  }

}
