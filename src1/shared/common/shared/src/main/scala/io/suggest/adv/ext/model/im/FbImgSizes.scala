package io.suggest.adv.ext.model.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.geom.d2.{INamedSize2di, MSize2di}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:50
 * Description: Шаблон статической модели размеров картинок Facebook.
 */
object FbImgSizes extends StringEnum[FbImgSize] {

  /** Community-страницы: page, event, group. */
  case object FbPostLink extends FbImgSize( "fbc" ) {
    override def whPx = MSize2di(
      width  = 1200,
      /** 630 по докам фейсбука, 628 по данным ссылки ниже.
        * @see [[https://blog.bufferapp.com/ideal-image-sizes-social-media-posts]] */
      height = 630
    )
  }

  override def values= findValues

}


sealed abstract class FbImgSize(override val value: String) extends StringEnumEntry with INamedSize2di {
  @deprecated def szAlias: String = value
}

object FbImgSize {

  implicit def fbImgSizeFormat: Format[FbImgSize] =
    EnumeratumUtil.valueEnumEntryFormat( FbImgSizes )

  @inline implicit def univEq: UnivEq[FbImgSize] = UnivEq.derive

}
