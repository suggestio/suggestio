package models.blk

import io.suggest.ym.model.common.BlockMeta
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable
import util.blocks.BlocksConf

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 11:01
 * Description: web-утиль для модели BlockMeta.
 */

object BlockMetaUtil {

  import play.api.data.Forms._

  /** Маппинг для интерфейса IBlockMeta. */
  def imapping: Mapping[IBlockMeta] = {
    mapping(
      "width"   -> BlockWidths.idMapping,
      "height"  -> BlockHeights.idMapping,
      "blockId" -> BlocksConf.idMapping,
      "wide"    -> boolean
    )
    { BlockMeta.apply(_, _, _, _) : IBlockMeta }
    { ibm => Some((ibm.width, ibm.height, ibm.blockId, ibm.wide)) }
  }

}

/** Трейт поддержки биндингов для IBlockMeta. */
trait IBlockMetaQsb {

  // TODO Надо бы выверять значения ширин, длин, id блока вместо использования intB.
  implicit def blockMetaQsb(implicit intB: QueryStringBindable[Int],
                            boolB: QueryStringBindable[Boolean]): QueryStringBindable[IBlockMeta] = {
    new QueryStringBindable[IBlockMeta] {
      def WIDTH_SUF     = ".a"
      def HEIGHT_SUF    = ".b"
      def BLOCK_ID_SUF  = ".c"
      def IS_WIDE_SUF   = ".d"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, IBlockMeta]] = {
        for {
          maybeWidth    <- intB.bind(key + WIDTH_SUF, params)
          maybeHeight   <- intB.bind(key + HEIGHT_SUF, params)
          maybeBlockId  <- intB.bind(key + BLOCK_ID_SUF, params)
          maybeIsWide   <- boolB.bind(key + IS_WIDE_SUF, params)
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

      override def unbind(key: String, value: IBlockMeta): String = {
        Iterator(
          intB.unbind(key + WIDTH_SUF,    value.width),
          intB.unbind(key + HEIGHT_SUF,   value.height),
          intB.unbind(key + BLOCK_ID_SUF, value.blockId),
          boolB.unbind(key + IS_WIDE_SUF, value.wide)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}
