package io.suggest.model.n2.media

import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.15 18:47
  * Description: Модель метаданных по картинке в рамках модели [[MMedia]].
  * Модель может использоваться как характеристики для файлов изображений, так и для видео-файлов.
  *
  * 2017-10-06: Модель полностью перепиливается: поля w и h выносятся в отдельное поле,
  * а сама становится неявно-пустой. Это решит кучу проблем, мешающих нормальному пользованию моделью.
  */
object MPictureMeta
  extends IEsMappingProps
  with IEmpty
{

  override type T = MPictureMeta
  override def empty = apply()

  object Fields {

    val WH_PX_FN        = "wh"
    val COLORS_FN       = "c"

  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.WH_PX_FN -> FObject.plain(
        enabled = someTrue,
        properties = Some(MSize2di.esMappingProps)
      ),
      F.COLORS_FN -> FObject.nested(
        properties = MColorData.esMappingProps,
      ),
    )
  }

  /** Поддержка play-json. */
  implicit val pictureMetaJson: OFormat[MPictureMeta] = {
    val F = Fields

    val modernFormat = (
      (__ \ F.WH_PX_FN).formatNullable[MSize2di] and
      (__ \ F.COLORS_FN).formatNullable[Seq[MColorData]]
        .inmap[Seq[MColorData]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { colors => if (colors.isEmpty) None else Some(colors) }
        )
    )(apply, unlift(unapply))

    // До 2017-10-06 модель почему-то повторяла собой MSize2di полностью... TODO Удалить после resaveMany().
    val compatReads = {
      for ( mSize2d <- implicitly[Reads[MSize2di]]) yield {
        MPictureMeta(
          whPx = Some(mSize2d)
        )
      }
    }

    OFormat(
      compatReads.orElse( modernFormat ),
      modernFormat
    )
  }

  @inline implicit def univEq: UnivEq[MPictureMeta] = {
    UnivEq.derive
  }


  val whPx = GenLens[MPictureMeta](_.whPx)
  val colors = GenLens[MPictureMeta](_.colors)

}


/** Неявно-пустой класс модели данных по картинке (видео, изображение).
  *
  * @param whPx Пиксельный двумерный размер картинки.
  * @param colors Основные цвета.
  */
case class MPictureMeta(
                         whPx       : Option[MSize2di]  = None,
                         colors     : Seq[MColorData]   = Nil
                       )
  extends EmptyProduct
