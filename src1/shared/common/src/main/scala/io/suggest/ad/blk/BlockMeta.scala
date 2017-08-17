package io.suggest.ad.blk

import io.suggest.common.geom.d2.ISize2di
import play.api.libs.functional.syntax._
import play.api.libs.json._


/** Модель метаданных по блоку рекламной карточки. */
object BlockMeta {

  val HEIGHT_ESFN   = "height"
  val WIDTH_ESFN    = "width"
  val WIDE_ESFN     = "wide"


  def DEFAULT = BlockMeta( h = BlockHeights.default, w = BlockWidths.default )

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[BlockMeta] = (
    (__ \ WIDTH_ESFN).format[BlockWidth] and
    (__ \ HEIGHT_ESFN).format[BlockHeight] and
    (__ \ WIDE_ESFN).format[Boolean]
  )(apply, unlift(unapply))


}


/**
 * Неизменяемое представление глобальных парамеров блока.
 * @param height высота блока.
 */
case class BlockMeta(
                      w         : BlockWidth,
                      h         : BlockHeight,
                      wide      : Boolean = false
                    )
  extends ISize2di
{

  def withWidth(w: BlockWidth) = copy(w = w)
  def withHeight(h: BlockHeight) = copy(h = h)
  def withWide(wide: Boolean) = copy(wide = wide)

  override def width = w.value
  override def height = h.value

}
