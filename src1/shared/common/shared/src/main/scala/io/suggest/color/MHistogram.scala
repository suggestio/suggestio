package io.suggest.color

import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.14 15:55
  * Description: Модель-обертка для коллекции с данными гистограммы.
  * Полезна при передаче гистограммы между акторами, у которых динамическая типизация в receive().
  */

object MHistogram
  extends IEsMappingProps
{

  object Fields {
    val COLORS_FN = "c"
  }

  /** Поддержка play-json. Используется в т.ч. для веб-сокетов. */
  implicit def histogramJson: OFormat[MHistogram] = {
    val F = Fields
    val mcdsJson = implicitly[Format[Seq[MColorData]]]

    val normalFormat = (__ \ F.COLORS_FN)
      .formatNullable( mcdsJson )
      .inmap[Seq[MColorData]](
        EmptyUtil.opt2ImplEmpty1F( Nil ),
        colors => Option.when(colors.nonEmpty)(colors)
      )

    // 2019-12-23 - Список цветов жил сам по себе, но теперь хранится MHistogram.
    val fallbackReads = Reads {
      case jsObj: JsObject =>
        jsObj.validate( normalFormat )
      case jsArr: JsArray =>
        jsArr.validate( mcdsJson )
      case other =>
        JsError( other.toString() )
    }

    OFormat( fallbackReads, normalFormat )
      .inmap[MHistogram]( apply, _.colors )
  }

  @inline implicit def univEq: UnivEq[MHistogram] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  val colors = GenLens[MHistogram](_.colors)


  implicit class MHistogramOpsExt( private val hist0: MHistogram ) extends AnyVal {

    def relFreqsCalculated: MHistogram = {
      val iter0 = hist0.colors
        .iterator
        .flatMap(_.count)

      if (iter0.isEmpty) {
        hist0
      } else {
        val totalCount = iter0.sum
        MHistogram.colors.modify { sorted0 =>
          for (e <- sorted0) yield {
            e.count.fold(e) { eCount =>
              (MColorData.freqPc set Some((eCount * 100 / totalCount).toInt) )(e)
            }
          }
        }(hist0)
      }
    }


    /** Сделать гистограмму, которая содержит только N первых по списку цветов.
      * Считаем, что массив отсортирован.
      */
    def shrinkColorsCount(maxColors: Int): MHistogram = {
      if (hist0.colors.sizeIs > maxColors) {
        MHistogram.colors
          .modify( _.take(maxColors) )(hist0)
      } else {
        hist0
      }
    }

  }


  implicit class MHistogramOptOpsExt( private val histOpt0: Option[MHistogram] ) extends AnyVal {

    def colorsOrNil: Seq[MColorData] =
      histOpt0.fold[Seq[MColorData]] (Nil) (_.colors)

  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.COLORS_FN -> FObject.nested(
        properties = MColorData.esMappingProps,
      ),
    )
  }

}


/** Класс модели гистограммы.
  *
  * @param colors Отсортированная гистограмма.
  */
case class MHistogram(
                       colors     : Seq[MColorData],
                     )
  extends EmptyProduct
{

  /** Выставить всем цветам freqPc но основе поля count. */
  lazy val withRelFrequences = this.relFreqsCalculated

}
