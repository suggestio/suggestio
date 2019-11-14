package io.suggest.jd

import io.suggest.ad.blk.{BlockPadding, BlockPaddings, BlockWidths}
import io.suggest.dev.{MSzMult, MSzMults}
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 14:59
  * Description: Модель общей конфигурации рендерера.
  */

object MJdConf {

  /** Поддержка play-json. */
  implicit def MJD_CONF_FORMAT: OFormat[MJdConf] = (
    (__ \ "e").format[Boolean] and
    (__ \ "z").format[MSzMult] and
    (__ \ "p").format[BlockPadding] and
    (__ \ "c").format[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MJdConf] = UnivEq.force

  val isEdit            = GenLens[MJdConf](_.isEdit)
  val szMult            = GenLens[MJdConf](_.szMult)
  val blockPadding      = GenLens[MJdConf](_.blockPadding)
  val gridColumnsCount  = GenLens[MJdConf](_.gridColumnsCount)

  /** Для рендера всяких списков карточек и прочего, обычно используется минимальный рендер. */
  def simpleMinimal = apply(
    isEdit            = false,
    szMult            = MSzMults.`1.0`,
    gridColumnsCount  = 2,
  )

}


/** Класс модели общей конфигурации рендеринга.
  *
  * @param isEdit Рендерить для редактора карточки.
  *                 Это означает, например, что некоторые элементы становятся перемещаемыми
  *                 и генерят соотв.события.
  * @param szMult Мультипликатор размера карточки.
  *               Его можно переопределить на уровне каждого конкретного блока.
  * @param blockPadding Настройка интервала между блоками плитки. Пока не реализована нормально.
  * @param gridColumnsCount Кол-во колонок в плитке.
  */
case class MJdConf(
                    isEdit              : Boolean,
                    szMult              : MSzMult,
                    blockPadding        : BlockPadding = BlockPaddings.default,
                    // TODO Это не уместно тут? Лучше убрать. JdR в редакторе зависит, т.к. рендерит плитку пока так.
                    gridColumnsCount    : Int
                  ) {

  /** Фактическая ширина внутреннего контейнера плитки в пикселях вместе с ободом вокруг. */
  lazy val gridWidthPx: Int = {
    val d = (gridColumnsCount / 2) *
      (BlockWidths.NORMAL.value + BlockPaddings.default.value) *
      szMult.toDouble
    d.toInt
  }

  /** Внутренняя ширина плитки без окружающего обода. */
  def gridInnerWidthPx: Int =
    gridWidthPx - BlockPaddings.default.fullBetweenBlocksPx

  /** Ширина wide-блока без фоновой картинки.  */
  def plainWideBlockWidthPx: Int =
    gridWidthPx + BlockPaddings.default.value * 2

  lazy val szMultF = MSzMult.szMultedF( szMult )

}

