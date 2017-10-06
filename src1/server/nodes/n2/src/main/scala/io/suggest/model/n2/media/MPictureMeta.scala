package io.suggest.model.n2.media

import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.geom.d2.{MSize2di, MSize2diEs}
import io.suggest.es.model.IGenEsMappingProps
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
object MPictureMeta extends IGenEsMappingProps with IEmpty {

  override type T = MPictureMeta
  override def empty = apply()

  object Fields {

    val WH_FN           = "wh"
    val MAIN_COLORS_FN  = "mc"

  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    val F = Fields
    List(
      FieldObject(F.WH_FN, enabled = true, properties = MSize2diEs.generateMappingProps),
      FieldObject(F.MAIN_COLORS_FN, enabled = false, properties = Nil)
    )
  }


  /** Поддержка play-json. */
  implicit val MPICTURE_META_FORMAT: OFormat[MPictureMeta] = {
    val F = Fields

    val modernFormat = (
      (__ \ F.WH_FN).formatNullable[MSize2di] and
      (__ \ F.MAIN_COLORS_FN).formatNullable[Seq[MColorData]]
        .inmap[Seq[MColorData]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { colors => if (colors.isEmpty) None else Some(colors) }
        )
    )(apply, unlift(unapply))

    // До 2017-10-06 модель почему-то повторяла собой MSize2di полностью... TODO Удалить после resaveMany().
    val compatReads = {
      for ( mSize2d <- implicitly[Reads[MSize2di]]) yield {
        MPictureMeta(
          whPxOpt = Some(mSize2d)
        )
      }
    }

    OFormat(
      compatReads.orElse( modernFormat ),
      modernFormat
    )
  }

  implicit def univEq: UnivEq[MPictureMeta] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Неявно-пустой класс модели данных по картинке (видео, изображение).
  *
  * @param whPxOpt Пиксельный двумерный размер картинки.
  * @param mainColors Основные цвета.
  */
case class MPictureMeta(
                         whPxOpt    : Option[MSize2di]  = None,
                         mainColors : Seq[MColorData]   = Nil
                       )
  extends EmptyProduct
