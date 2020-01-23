package io.suggest.n2.media

import enumeratum.values.{ShortEnum, ShortEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.2020 22:05
  * Description: Флаги для хэшей.
  */
object MFileMetaHashFlags extends ShortEnum[MFileMetaHashFlag] {

  case object TrulyOriginal extends MFileMetaHashFlag( 1 )

  case object LossLessDerivative extends MFileMetaHashFlag( 2 )


  override def values = findValues

}


sealed abstract class MFileMetaHashFlag(override val value: Short) extends ShortEnumEntry

object MFileMetaHashFlag {

  @inline implicit def univEq: UnivEq[MFileMetaHashFlag] = UnivEq.derive

  implicit def fileMetaHashJson: Format[MFileMetaHashFlag] =
    EnumeratumUtil.valueEnumEntryFormat( MFileMetaHashFlags )

}
