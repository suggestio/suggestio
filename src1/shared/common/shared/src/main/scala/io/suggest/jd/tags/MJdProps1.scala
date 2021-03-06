package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeight, BlockHeights, BlockMeta, BlockWidth, BlockWidths, MBlockExpandMode}
import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdEdgeId
import io.suggest.text.StringUtil
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
object MJdProps1 extends IEmpty {

  override type T = MJdProps1

  override val empty = apply()

  /** Поддержка play-json. */
  implicit def jdtProps1Format: OFormat[MJdProps1] = {
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
      (__ \ "l").formatNullable[Int] and
      (__ \ "o").formatNullable[MJdOutLine]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MJdProps1] = UnivEq.derive

  def bgColor     = GenLens[MJdProps1](_.bgColor)
  def bgImg       = GenLens[MJdProps1](_.bgImg)
  def topLeft     = GenLens[MJdProps1](_.topLeft)
  def isMain      = GenLens[MJdProps1](_.isMain)
  def rotateDeg   = GenLens[MJdProps1](_.rotateDeg)
  def textShadow  = GenLens[MJdProps1](_.textShadow)
  def widthPx     = GenLens[MJdProps1](_.widthPx)
  def heightPx    = GenLens[MJdProps1](_.heightPx)
  def expandMode  = GenLens[MJdProps1](_.expandMode)
  def lineHeight  = GenLens[MJdProps1](_.lineHeight)
  def outline     = GenLens[MJdProps1](_.outline)


  object LineHeight {
    def MIN = 2
    def MAX = 300
    def isValid(lineHeight: Int): Boolean =
      (lineHeight >= 2) || (lineHeight <= MAX)
  }


  implicit final class JdtP1Ext( private val jdP1: MJdProps1 ) extends AnyVal {

    def wh: Option[MSize2di] = {
      for (w <- jdP1.widthPx; h <- jdP1.heightPx)
        yield MSize2di(w, height = h)
    }

    def isContentCssStyled: Boolean =
      jdP1.lineHeight.nonEmpty

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
  * @param outline Поле с данными обводки.
  */
final case class MJdProps1(
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
                            outline    : Option[MJdOutLine]        = None,
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

  override def toString: String = {
    StringUtil.toStringHelper(this, 128) { renderF =>
      bgColor foreach renderF("bgC")
      bgImg foreach renderF("bgI")
      topLeft foreach renderF("xy")
      isMain foreach renderF("main")
      rotateDeg foreach renderF("rot")
      textShadow foreach renderF("sha")
      widthPx foreach renderF("w")
      heightPx foreach renderF("h")
      expandMode foreach renderF("exp")
      lineHeight foreach renderF("lh")
      outline foreach renderF("ol")
    }
  }

}
