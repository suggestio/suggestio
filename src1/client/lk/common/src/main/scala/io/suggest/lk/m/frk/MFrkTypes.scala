package io.suggest.lk.m.frk

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.18 11:22
  * Description: Модель типов картинки в рамках [[MFormResourceKey]].
  */
object MFrkTypes extends Enum[MFrkType] {

  /** Картинка логотипа узла. */
  case object Logo extends MFrkType

  /** Картинка приветствия переднего плана. */
  case object WcFg extends MFrkType

  /** Картинки галереи узла. */
  case object GalImg extends MFrkType


  override def values = findValues

}


sealed abstract class MFrkType extends EnumEntry

object MFrkType {

  implicit def univEq: UnivEq[MFrkType] = UnivEq.derive

}
