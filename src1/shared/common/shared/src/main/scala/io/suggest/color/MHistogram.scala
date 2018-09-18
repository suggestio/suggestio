package io.suggest.color

import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.14 15:55
  * Description: Модель-обертка для коллекции с данными гистограммы.
  * Полезна при передаче гистограммы между акторами, у которых динамическая типизация в receive().
  */

object MHistogram {

  object Fields {
    val COLORS_FN = "c"
  }

  /** Поддержка play-json. Используется в т.ч. для веб-сокетов. */
  implicit val MHISTOGRAM_FORMAT: OFormat[MHistogram] = {
    val F = Fields
    (__ \ F.COLORS_FN).formatNullable[Seq[MColorData]]
      .inmap[Seq[MColorData]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { colors => if (colors.isEmpty) None else Some(colors) }
      )
      .inmap[MHistogram](apply, _.sorted)
  }

  @inline implicit def univEq: UnivEq[MHistogram] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Класс модели гистограммы.
  *
  * @param sorted Отсортированная гистограмма.
  */
case class MHistogram(
                       sorted: Seq[MColorData]
                     )
  extends EmptyProduct
{

  /** Выставить всем цветам freqPc но основе поля count. */
  lazy val withRelFrequences: MHistogram = {
    val iter0 = sorted
      .iterator
      .flatMap(_.count)

    if (iter0.isEmpty) {
      this
    } else {
      val totalCount = iter0.sum
      withSorted(
        sorted = for (e <- sorted) yield {
          e.count.fold(e) { eCount =>
            e.withFreqPc(
              Some((eCount * 100 / totalCount).toInt)
            )
          }
        }
      )
    }
  }


  /** Сделать гистограмму, которая содержит только N первых по списку цветов.
    * Считаем, что массив отсортирован.
    */
  def shrinkColorsCount(maxColors: Int): MHistogram = {
    if (sorted.size > maxColors) {
      withSorted(
        sorted.take(maxColors)
      )
    } else {
      this
    }
  }


  def withSorted(sorted: Seq[MColorData]) = copy(sorted = sorted)

}
