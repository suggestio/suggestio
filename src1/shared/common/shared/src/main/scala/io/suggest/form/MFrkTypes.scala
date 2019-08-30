package io.suggest.form

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


  object somes {
    lazy val LogoSome = Some( Logo )
    lazy val WcFgSome = Some( WcFg )
    lazy val GalImgSome = Some( MFrkTypes.GalImg )
  }
}


sealed abstract class MFrkType extends EnumEntry

object MFrkType {

  @inline implicit def univEq: UnivEq[MFrkType] = UnivEq.derive

}
