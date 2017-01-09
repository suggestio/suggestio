package io.suggest.model.n2.ad.blk

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.ym.model.common.MImgSizeT
import io.suggest.common.empty.EmptyUtil._
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable

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
      .inmap [Int] (_.getOrElse(WIDTH_DFLT), someF) and
    (__ \ WIDE_ESFN).formatNullable[Boolean]
      .inmap [Boolean] (_.getOrElse(false), someF)
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


  /** Поддержка сериализации/десериализации в URL query string. */
  // TODO Надо бы выверять значения ширин, длин, id блока вместо использования intB.
  implicit def blockMetaQsb(implicit
                            intB: QueryStringBindable[Int],
                            boolB: QueryStringBindable[Boolean]
                           ): QueryStringBindable[BlockMeta] = {
    new QueryStringBindableImpl[BlockMeta] {
      def WIDTH_FN     = "a"
      def HEIGHT_FN    = "b"
      def BLOCK_ID_FN  = "c"
      def IS_WIDE_FN   = "d"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BlockMeta]] = {
        val k = key1F(key)
        for {
          maybeWidth    <- intB.bind  (k(WIDTH_FN), params)
          maybeHeight   <- intB.bind  (k(HEIGHT_FN), params)
          maybeBlockId  <- intB.bind  (k(BLOCK_ID_FN), params)
          maybeIsWide   <- boolB.bind (k(IS_WIDE_FN), params)
        } yield {
          for {
            width   <- maybeWidth.right
            height  <- maybeHeight.right
            blockId <- maybeBlockId.right
            isWide  <- maybeIsWide.right
          } yield {
            BlockMeta(
              width   = width,
              height  = height,
              blockId = blockId,
              wide    = isWide
            )
          }
        }
      }

      override def unbind(key: String, value: BlockMeta): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            intB.unbind ( k(WIDTH_FN),     value.width),
            intB.unbind ( k(HEIGHT_FN),    value.height),
            intB.unbind ( k(BLOCK_ID_FN),  value.blockId),
            boolB.unbind( k(IS_WIDE_FN),   value.wide)
          )
        }
      }
    }
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
  override val blockId : Int,
  override val height  : Int,
  override val width   : Int,
  override val wide    : Boolean = false
)
  extends IBlockMeta
