package io.suggest.n2.media

import io.suggest.color.MHistogram
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.15 18:47
  * Description: Модель метаданных по картинке в рамках модели MMedia, переехавшая в MEdgeMedia,
  * и заодно на клиент.
  * Модель может использоваться как характеристики для файлов изображений, так и для видео-файлов.
  */
object MPictureMeta
  extends IEsMappingProps
  with IEmpty
{

  override type T = MPictureMeta
  override val empty = apply()

  object Fields {

    val WH_PX_FN        = "wh"
    val HISTOGRAM_FN    = "hst"

    object Wh extends PrefixedFn {
      import MSize2di.{Fields => F}
      override protected def _PARENT_FN = WH_PX_FN
      def WIDTH_FN = _fullFn( F.WIDTH_FN )
      def HEIGHT_FN = _fullFn( F.HEIGHT_FN )
    }

  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.WH_PX_FN -> FObject.plain( MSize2di ),
      F.HISTOGRAM_FN -> FObject.plain( MHistogram ),
    )
  }

  /** Поддержка play-json. */
  implicit def pictureMetaJson: OFormat[MPictureMeta] = {
    val F = Fields

    val modernFormat = (
      (__ \ F.WH_PX_FN).formatNullable[MSize2di] and
      (__ \ F.HISTOGRAM_FN).formatNullable[MHistogram]
    )(apply, unlift(unapply))

    // До 2017-10-06 модель почему-то повторяла собой MSize2di полностью... TODO Удалить после resaveMany().
    val compatReads = {
      for {
        mSize2d <- implicitly[Reads[MSize2di]]
      } yield {
        MPictureMeta(
          whPx = Some(mSize2d),
        )
      }
    }

    OFormat(
      compatReads.orElse( modernFormat ),
      modernFormat
    )
  }

  @inline implicit def univEq: UnivEq[MPictureMeta] = UnivEq.derive


  def whPx      = GenLens[MPictureMeta](_.whPx)
  def histogram = GenLens[MPictureMeta](_.histogram)

}


/** Неявно-пустой класс модели данных по картинке (видео, изображение).
  *
  * @param whPx Пиксельный двумерный размер картинки.
  * @param histogram Основные цвета.
  */
final case class MPictureMeta(
                               whPx       : Option[MSize2di]      = None,
                               histogram  : Option[MHistogram]    = None,
                             )
  extends EmptyProduct
{

  override def toString: String = {
    if (isEmpty) {
      ""
    } else {
      val sb = new StringBuilder(64, productPrefix)
        .append('(')

      for (wh <- whPx)
        sb.append("wh=").append(wh)

      for (hist <- histogram)
        sb.append(' ')
          .append("hist=")
          .append(hist)

      sb.append(')')
        .toString()
    }
  }

}
