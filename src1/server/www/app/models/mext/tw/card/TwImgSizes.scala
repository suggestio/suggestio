package models.mext.tw.card

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.geom.d2.{INamedSize2di, MSize2di}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 18:42
 * Description: Размеры картинок для карточек твиттера.
 */
object TwImgSizes extends StringEnum[TwImgSize] {

  /** Размер картинки для фотокарточек. */
  case object Photo extends TwImgSize("p") {

    override def whPx = MSize2di(
      width  = 1024,
      height = 512
    )

  }

  override def values = findValues

}


sealed abstract class TwImgSize(override val value: String) extends StringEnumEntry with INamedSize2di

object TwImgSize {

  implicit def twImgSizeFormat: Format[TwImgSize] =
    EnumeratumUtil.valueEnumEntryFormat( TwImgSizes )

  @inline implicit def univEq: UnivEq[TwImgSize] = UnivEq.derive

}
