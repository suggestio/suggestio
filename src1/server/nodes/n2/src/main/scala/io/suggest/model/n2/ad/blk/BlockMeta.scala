package io.suggest.model.n2.ad.blk

import io.suggest.common.geom.d2.ISize2di
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable

/** Модель метаданных по блоку рекламной карточки. */
object BlockMeta extends IGenEsMappingProps {

  val HEIGHT_ESFN   = "height"
  val WIDTH_ESFN    = "width"
  val WIDE_ESFN     = "wide"


  val DEFAULT = BlockMeta( height = 300, width = 300 )

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[BlockMeta] = (
    (__ \ HEIGHT_ESFN).format[Int] and
    (__ \ WIDTH_ESFN).format[Int] and
    (__ \ WIDE_ESFN).format[Boolean]
  )(apply, unlift(unapply))


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
      _fint(HEIGHT_ESFN),
      _fint(WIDTH_ESFN),
      FieldBoolean(WIDE_ESFN, index = true, include_in_all = false)
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
      def IS_WIDE_FN   = "d"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BlockMeta]] = {
        val k = key1F(key)
        for {
          maybeWidth    <- intB.bind  (k(WIDTH_FN), params)
          maybeHeight   <- intB.bind  (k(HEIGHT_FN), params)
          maybeIsWide   <- boolB.bind (k(IS_WIDE_FN), params)
        } yield {
          for {
            width   <- maybeWidth.right
            height  <- maybeHeight.right
            isWide  <- maybeIsWide.right
          } yield {
            BlockMeta(
              width   = width,
              height  = height,
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
            boolB.unbind( k(IS_WIDE_FN),   value.wide)
          )
        }
      }
    }
  }

}


/**
 * Неизменяемое представление глобальных парамеров блока.
 * @param height высота блока.
 */
case class BlockMeta(
                      override val height  : Int,
                      override val width   : Int,
                      wide                 : Boolean = false
                    )
  extends ISize2di
