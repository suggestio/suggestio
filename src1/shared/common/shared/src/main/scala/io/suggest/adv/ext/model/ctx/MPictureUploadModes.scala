package io.suggest.adv.ext.model.ctx

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 14:53
 * Description: Модель допустимых режимов загрузки картинки в хранилища внешних сервисов.
 */
object MPictureUploadModes extends StringEnum[MPictureUploadMode] {

  case object S2s extends MPictureUploadMode( "s2s" )

  override def values = findValues

}


sealed abstract class MPictureUploadMode(override val value: String) extends StringEnumEntry

object MPictureUploadMode {

  implicit def mPictureUploadModeFormat: Format[MPictureUploadMode] =
    EnumeratumUtil.valueEnumEntryFormat( MPictureUploadModes )

  @inline implicit def univEq: UnivEq[MPictureUploadMode] = UnivEq.derive

}

