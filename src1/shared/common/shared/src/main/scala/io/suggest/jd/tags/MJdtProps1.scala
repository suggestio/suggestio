package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths, MBlockExpandMode}
import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdEdgeId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 13:03
  * Description: Кросс-платформенная унифицированная JSON-модель пропертисов унифицированных jd-тегов.
  * 1 - потому что в будущем пропертисов будет больше, и наверное моделей тоже будет больше.
  *
  * Неявно-пустая модель.
  */
object MJdtProps1 extends IEmpty {

  override type T = MJdtProps1

  override def empty = MJdtProps1()

  /** Поддержка play-json. */
  implicit def jdtProps1Format: OFormat[MJdtProps1] = {
    val bm = (__ \ "c").formatNullable[BlockMeta]

    (
      (__ \ "a").formatNullable[MColorData] and
      (__ \ "b").formatNullable[MJdEdgeId] and
      //(__ \ "c").formatNullable[BlockMeta] and
      (__ \ "d").formatNullable[MCoords2di] and
      (__ \ "e").formatNullable[Boolean] and
      (__ \ "g").formatNullable[Int] and
      (__ \ "s").formatNullable[MJdShadow] and {
        // TODO 2019.10.22 После resaveAll() заменить на (__ \ "f").formatNullable[Int]
        val path = (__ \ "f")
        val r = path.read[Int].map(Option.apply) orElse bm.map(_.map(_.width))
        OFormat(r, path.writeNullable[Int])
      } and {
        // TODO 2019.10.22 После resaveAll() заменить на (__ \ "h").formatNullable[Int]
        val path = (__ \ "h")
        val r = path.read[Int].map(Option.apply) orElse bm.map(_.map(_.height))
        OFormat(r, path.writeNullable[Int])
      } and {
        // TODO 2019.10.22 После resaveAll() заменить на (__ \ "x").formatNullable[MBlockExpandMode]
        val path = (__ \ "x")
        val r = path.read[MBlockExpandMode].map(Option.apply) orElse bm.map(_.flatMap(_.expandMode))
        OFormat(r, path.writeNullable[MBlockExpandMode])
      }
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdtProps1] = UnivEq.derive

  val bgColor     = GenLens[MJdtProps1](_.bgColor)
  val bgImg       = GenLens[MJdtProps1](_.bgImg)
  //val bm          = GenLens[MJdtProps1](_.bm)
  val topLeft     = GenLens[MJdtProps1](_.topLeft)
  val isMain      = GenLens[MJdtProps1](_.isMain)
  val rotateDeg   = GenLens[MJdtProps1](_.rotateDeg)
  val textShadow  = GenLens[MJdtProps1](_.textShadow)
  val widthPx     = GenLens[MJdtProps1](_.widthPx)
  val heightPx    = GenLens[MJdtProps1](_.heightPx)
  val expandMode  = GenLens[MJdtProps1](_.expandMode)

}


/** Класс модели-контейнера пропертисов унифицированных jd-тегов.
  *
  * @param bgColor Цвет фона элемента.
  * @param bgImg Фоновая картинка элемента.
  * @param topLeft css-px-координаты левого верхнего угла элемента для абсолютного позиционирования.
  * @param isMain Абстрактный флаг высшей приоритетности среди равных.
  *               В контексте стрипа обозначает, что данный стрип главный/заглавный, и именно он должен быть
  *               использован для рендера в плитке среди других карточек.
  * @param rotateDeg Угол поворота в градусах, если задан.
  * @param widthPx Ширина в пикселях.
  *                Изначально была только для qd-контента, а ширина блока жила отдельно в bm.width .
  * @param heightPx Высота. Вынесена из BlockMeta.height.
  * @param expandMode Режим расширения тега. Вынесено из BlockMeta.expandMode
  */
case class MJdtProps1(
                       bgColor    : Option[MColorData]        = None,
                       bgImg      : Option[MJdEdgeId]         = None,
                       topLeft    : Option[MCoords2di]        = None,
                       isMain     : Option[Boolean]           = None,
                       rotateDeg  : Option[Int]               = None,
                       textShadow : Option[MJdShadow]         = None,
                       widthPx    : Option[Int]               = None,
                       heightPx   : Option[Int]               = None,
                       expandMode : Option[MBlockExpandMode]  = None,
                     )
  extends EmptyProduct
{

  /** BlockMeta для блоков (стрипов). */
  lazy val bm: Option[BlockMeta] = {
    for {
      width <- widthPx
      w <- BlockWidths.withValueOpt( width )
      height <- heightPx
      h <- BlockHeights.withValueOpt( height )
    } yield {
      BlockMeta(w, h, expandMode)
    }
  }

  def wh: Option[MSize2di] = {
    for (w <- widthPx; h <- heightPx)
      yield MSize2di(w, height = h)
  }

}
