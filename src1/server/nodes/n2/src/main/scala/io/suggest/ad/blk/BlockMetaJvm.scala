package io.suggest.ad.blk

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.playx.FormMappingUtil
import play.api.data.Mapping
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 14:15
  * Description: Доп.поддержка кросс-платформенной модели BlockMeta на стороне JVM.
  */
object BlockMetaJvm extends IGenEsMappingProps {

  import io.suggest.es.util.SioEsUtil._

  private def _fint(fn: String) = {
    FieldNumber(
      id              = fn,
      fieldType       = DocFieldTypes.integer,
      index           = true,
      include_in_all  = false
    )
  }

  def generateMappingProps: List[DocField] = {
    List(
      _fint( BlockMeta.HEIGHT_ESFN),
      _fint( BlockMeta.WIDTH_ESFN),
      FieldBoolean( BlockMeta.WIDE_ESFN, index = true, include_in_all = false),
    )
  }


  /** Поддержка QSB для модели BlockWidths. */
  implicit def blockWidthQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[BlockWidth] = {
    EnumeratumJvmUtil.valueEnumQsb( BlockWidths )
  }


  /** Поддержка QSB для модели BlockHeights. */
  implicit def blockHeightQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[BlockHeight] = {
    EnumeratumJvmUtil.valueEnumQsb( BlockHeights )
  }


  /** Поддержка сериализации/десериализации в URL query string. */
  implicit def blockMetaQsb(implicit
                            blockWidthB   : QueryStringBindable[BlockWidth],
                            blockHeightB  : QueryStringBindable[BlockHeight],
                            boolB         : QueryStringBindable[Boolean]
                           ): QueryStringBindable[BlockMeta] = {
    new QueryStringBindableImpl[BlockMeta] {
      def WIDTH_FN     = "a"
      def HEIGHT_FN    = "b"
      def IS_WIDE_FN   = "d"
      def HEIGHT_GROW_FN = "e"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BlockMeta]] = {
        val k = key1F(key)
        for {
          bWidthE     <- blockWidthB.bind  (k(WIDTH_FN), params)
          bHeightE    <- blockHeightB.bind (k(HEIGHT_FN), params)
          isWideE     <- boolB.bind        (k(IS_WIDE_FN), params)
        } yield {
          for {
            width         <- bWidthE.right
            height        <- bHeightE.right
            isWide        <- isWideE.right
          } yield {
            BlockMeta(
              w           = width,
              h           = height,
              wide        = isWide,
            )
          }
        }
      }

      override def unbind(key: String, value: BlockMeta): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          blockWidthB.unbind  ( k(WIDTH_FN),     value.w),
          blockHeightB.unbind ( k(HEIGHT_FN),    value.h),
          boolB.unbind        ( k(IS_WIDE_FN),   value.wide),
        )
      }
    }
  }

  def blockHeightMapping: Mapping[BlockHeight] = {
    FormMappingUtil.optMapping2required(
      EnumeratumJvmUtil.intIdOptMapping( BlockHeights ) )
  }

  def blockWidthMapping: Mapping[BlockWidth] = {
    FormMappingUtil.optMapping2required(
      EnumeratumJvmUtil.intIdOptMapping( BlockWidths ) )
  }

  /** Маппинг для интерфейса IBlockMeta. */
  def formMapping: Mapping[BlockMeta] = {
    import play.api.data.Forms._
    mapping(
      "width"   -> blockWidthMapping,
      "height"  -> blockHeightMapping,
      "wide"    -> boolean,
    )
    { BlockMeta.apply }
    { BlockMeta.unapply }
  }

}
