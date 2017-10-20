package io.suggest.ad.blk

import io.suggest.common.geom.d2.ISize2di
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scalaz.{Validation, ValidationNel}


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


  implicit def univEq: UnivEq[BlockMeta] = UnivEq.derive

  /** STUB: Валидация инстансов BlockMeta. */
  def validate(bm: BlockMeta): ValidationNel[String, BlockMeta] = {
    // Все поля модели очень жестко типизированы, поэтому валидировать нечего.
    Validation.success(bm)
  }

}


/**
 * Класс модели парамеров блока (стрипа).
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
