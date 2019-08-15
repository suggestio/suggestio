package io.suggest.ad.blk

import io.suggest.common.geom.d2.ISize2di
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}
import io.suggest.common.empty.OptionUtil.BoolOptJsonFormatOps


/** Модель метаданных по блоку рекламной карточки. */
object BlockMeta {

  val HEIGHT_ESFN   = "height"
  val WIDTH_ESFN    = "width"
  val WIDE_ESFN     = "wide"


  def DEFAULT = BlockMeta( h = BlockHeights.default, w = BlockWidths.default )

  /** Поддержка JSON. */
  implicit def blockMetaJson: OFormat[BlockMeta] = (
    (__ \ WIDTH_ESFN).format[BlockWidth] and
    (__ \ HEIGHT_ESFN).format[BlockHeight] and
    (__ \ WIDE_ESFN).formatNullable[Boolean].formatBooleanOrFalse
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[BlockMeta] = UnivEq.derive

  /** STUB: Валидация инстансов BlockMeta. */
  def validate(bm: BlockMeta): ValidationNel[String, BlockMeta] = {
    // Все поля модели очень жестко типизированы, поэтому валидировать нечего.
    Validation.success(bm)
  }


  def MINIMAL = BlockMeta( BlockWidths.min, BlockHeights.min )

  val w = GenLens[BlockMeta](_.w)
  val h = GenLens[BlockMeta](_.h)
  val wide = GenLens[BlockMeta](_.wide)


  implicit class BmOptExt(val bmOpt: Option[BlockMeta]) extends AnyVal {
    def wide: Boolean = bmOpt.exists(_.wide)
  }

}


/**
 * Класс модели парамеров блока (стрипа).
 */
case class BlockMeta(
                      w         : BlockWidth,
                      h         : BlockHeight,
                      wide      : Boolean       = false,
                    )
  extends ISize2di
{

  override def width = w.value
  override def height = h.value

}
