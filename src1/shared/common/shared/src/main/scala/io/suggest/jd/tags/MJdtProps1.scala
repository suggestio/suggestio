package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeight, BlockHeights, BlockMeta, BlockWidth, BlockWidths, MBlockExpandMode}
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

  override def empty = apply()

  /** Поддержка play-json. */
  implicit def jdtProps1Format: OFormat[MJdtProps1] = {
    (
      (__ \ "a").formatNullable[MColorData] and
      (__ \ "b").formatNullable[MJdEdgeId] and
      (__ \ "d").formatNullable[MCoords2di] and
      (__ \ "e").formatNullable[Boolean] and
      (__ \ "g").formatNullable[Int] and
      (__ \ "s").formatNullable[MJdShadow] and
      (__ \ "f").formatNullable[Int] and
      (__ \ "h").formatNullable[Int] and
      (__ \ "x").formatNullable[MBlockExpandMode] and
      (__ \ "l").formatNullable[Int]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdtProps1] = UnivEq.derive

  val bgColor     = GenLens[MJdtProps1](_.bgColor)
  val bgImg       = GenLens[MJdtProps1](_.bgImg)
  val topLeft     = GenLens[MJdtProps1](_.topLeft)
  val isMain      = GenLens[MJdtProps1](_.isMain)
  val rotateDeg   = GenLens[MJdtProps1](_.rotateDeg)
  val textShadow  = GenLens[MJdtProps1](_.textShadow)
  val widthPx     = GenLens[MJdtProps1](_.widthPx)
  val heightPx    = GenLens[MJdtProps1](_.heightPx)
  val expandMode  = GenLens[MJdtProps1](_.expandMode)
  val lineHeight  = GenLens[MJdtProps1](_.lineHeight)


  object LineHeight {
    def MIN = 2
    def MAX = 300
    def isValid(lineHeight: Int): Boolean =
      (lineHeight >= 2) || (lineHeight <= MAX)
  }

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
  * @param lineHeight Межстрочный интервал.
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
                       lineHeight : Option[Int]               = None,
)
  extends EmptyProduct
{

  lazy val blockWidth: Option[BlockWidth] =
    widthPx.flatMap(BlockWidths.withValueOpt)

  lazy val blockHeights: Option[BlockHeight] =
    heightPx.flatMap(BlockHeights.withValueOpt)

  /** Старый BlockMeta, который изначально был только для блоков (стрипов). */
  lazy val bm: Option[BlockMeta] = {
    for {
      w <- blockWidth
      h <- blockHeights
    } yield {
      BlockMeta(w, h, expandMode)
    }
  }

  def wh: Option[MSize2di] = {
    for (w <- widthPx; h <- heightPx)
      yield MSize2di(w, height = h)
  }

  def isContentCssStyled: Boolean =
    lineHeight.nonEmpty

}
