package io.suggest.model.n2.ad.blk

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.ym.model.common.MImgSizeT
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Модель метаданных по блоку рекламной карточки. */

object BlockMeta extends IGenEsMappingProps {

  val BLOCK_ID_ESFN = "blockId"
  val HEIGHT_ESFN   = "height"
  val WIDTH_ESFN    = "width"
  val WIDE_ESFN     = "wide"

  /** Поле ширины долго жило в настройках блока, а когда пришло время переезжать, возникла проблема с дефолтовым значением. */
  def WIDTH_DFLT = 300

  val DEFAULT = BlockMeta(blockId = 20, height = 300, width = WIDTH_DFLT)

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[BlockMeta] = (
    (__ \ BLOCK_ID_ESFN).format[Int] and
    (__ \ HEIGHT_ESFN).format[Int] and
    (__ \ WIDTH_ESFN).formatNullable[Int]
      .inmap [Int] (_ getOrElse WIDTH_DFLT,  Some.apply) and
    (__ \ WIDE_ESFN).formatNullable[Boolean]
      .inmap [Boolean] (_ getOrElse false, Some.apply)
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  private def _fint(fn: String) = {
    FieldNumber(
      id              = fn,
      fieldType       = DocFieldTypes.integer,
      index           = FieldIndexingVariants.not_analyzed,
      include_in_all  = false
    )
  }

  def generateMappingProps: List[DocField] = {
    List(
      _fint(BLOCK_ID_ESFN),
      _fint(HEIGHT_ESFN),
      _fint(WIDTH_ESFN),
      FieldBoolean(WIDE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/** Интерфейс для экземпляром BlockMeta. */
trait IBlockMeta extends MImgSizeT {

  /** id шаблона блока. */
  def blockId   : Int

  /** Использовать широкое отображение? */
  def wide      : Boolean

}



/**
 * Неизменяемое представление глобальных парамеров блока.
 * @param blockId id блока согласно BlocksConf.
 * @param height высота блока.
 */
case class BlockMeta(
  blockId : Int,
  height  : Int,
  width   : Int,
  wide    : Boolean = false
) extends IBlockMeta {

}
