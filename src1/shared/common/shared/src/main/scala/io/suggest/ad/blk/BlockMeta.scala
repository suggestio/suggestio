package io.suggest.ad.blk

import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.ISize2di
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.common.empty.OptionUtil.BoolOptOps


/** Модель метаданных по блоку рекламной карточки.
  * 2019-10-22 Модель стала живущей вне БД, переведена в раздел устаревших.
  * Надо от неё потихоньку избавляться.
  */
object BlockMeta {

  object Fields {
    def HEIGHT          = "height"
    def WIDTH           = "width"
    /** Старое название поля флага wide, который стал одним из значений RENDER_MODE. */
    private[BlockMeta] def WIDE    = "wide"
    def EXPAND_MODE     = "rm"
  }


  def DEFAULT = BlockMeta( h = BlockHeights.default, w = BlockWidths.default )

  /** Поддержка JSON. */
  implicit def blockMetaJson: OFormat[BlockMeta] = {
    val F = Fields

    val renderModeFormat = {
      val renderModeFormat0 = (__ \ F.EXPAND_MODE).formatNullable[MBlockExpandMode]
      val renderModeReadsFallbackWide = renderModeFormat0.filter(_.nonEmpty).orElse {
        (__ \ F.WIDE).readNullable[Boolean].map { wideOpt =>
          OptionUtil.maybe( wideOpt.getOrElseFalse )( MBlockExpandModes.Wide )
        }
      }
      OFormat( renderModeReadsFallbackWide, renderModeFormat0 )
    }

    (
      (__ \ F.WIDTH).format[BlockWidth] and
      (__ \ F.HEIGHT).format[BlockHeight] and
      renderModeFormat
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[BlockMeta] = UnivEq.derive


  def MINIMAL = BlockMeta( BlockWidths.min, BlockHeights.min )

  implicit class BmOptExt(val bmOpt: Option[BlockMeta]) extends AnyVal {
    def hasExpandMode: Boolean = bmOpt.exists( _.expandMode.nonEmpty )
  }

}


/** Класс модели парамеров блока (стрипа).
  *
  * @param w Ширина блока из допустимого ряда.
  * @param h Высота блока из допустимого ряда.
  * @param expandMode Режим растягивания блока.
  */
case class BlockMeta(
                      w               : BlockWidth,
                      h               : BlockHeight,
                      expandMode      : Option[MBlockExpandMode] = None,
                    )
  extends ISize2di
{

  override def width = w.value
  override def height = h.value

}
