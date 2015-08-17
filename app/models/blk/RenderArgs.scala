package models.blk

import io.suggest.common.css.ITopLeft
import models._
import models.im.make.{MakeResult, IMakeResult}
import models.msc.{MScApiVsns, MScApiVsn}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 18:42
 * Description: Модель для параметров рендера блоков. Изначально жила в models.Stuff.
 */

trait IRenderArgs {
  /** Рекламная карточка, которую надо отрендерить. */
  def mad             : MAdT
  /** Мультипликатор размеров карточки. */
  def szMult          : SzMult_t
  /** Данные о фоновой картинке, если есть. */
  def bgImg           : Option[IMakeResult]
  /** Рендерим в редакторе. */
  def withEdit        : Boolean
  /** Рендерить стили инлайново? */
  def inlineStyles    : Boolean
  /** Дополнительные css-классы, которые относятся к рендеру. */
  def cssClasses      : Seq[String]
  /** Стили для div .sm-block. */
  def topLeft         : Option[ITopLeft]
  /** Версия API выдачи. */
  def apiVsn          : MScApiVsn = MScApiVsns.unknownVsn
  /** Порядковый номер. Заполняется только для плитки выдачи. */
  def indexOpt        : Option[Int]

  /**
   * compat, потому и final.
   * Код этого метода -- теперь wrapper над bgImg, раньше же тут был ориджинал контент (WideCtx или что-то подобное).
   *
   * Рендерим бэкграунд на широкую. Если у карточки разрешен просмотр на широкую, то фон будет отрендерен
   * вне блока, широким, а тело блока сдвинуто согласно кропу.
   */
  final lazy val wideBg: Option[IMakeResult] = {
    bgImg.filter(_.isWide)
  }

}

/** Неполный враппер для [[IRenderArgs]]. Нужен, т.к. не везде допустимо враппать поле cssClasses. */
sealed trait IRenderArgsWrapper0 extends IRenderArgs {
  def brArgs: IRenderArgs

  override def mad          = brArgs.mad
  override def withEdit     = brArgs.withEdit
  override def szMult       = brArgs.szMult
  override def topLeft      = brArgs.topLeft
  override def bgImg        = brArgs.bgImg
  override def inlineStyles = brArgs.inlineStyles

  override def cssClasses   = brArgs.cssClasses
  override def indexOpt     = brArgs.indexOpt
  override def apiVsn       = brArgs.apiVsn
}


/** Параметры рендера блока. Дефолтовая реализация [[IRenderArgs]]. */
case class RenderArgs(
  override val mad            : MAd,
  override val szMult         : SzMult_t,
  override val bgImg          : Option[MakeResult],
  override val withEdit       : Boolean                 = false,
  override val inlineStyles   : Boolean                 = true,
  override val cssClasses     : Seq[String]             = Nil,
  override val topLeft        : Option[ITopLeft]        = None,
  override val indexOpt       : Option[Int]             = None,
  override val apiVsn         : MScApiVsn               = MScApiVsns.unknownVsn
)
  extends IRenderArgs



/** Интерфейс аргументов шаблона _blockStyleCss для рендера стиля блока. */
trait FieldCssRenderArgsT extends IRenderArgs {
  def aovf          : AOValueField
  def bf            : BlockAOValueFieldT
  def fieldCssClass : String
  def xy            : ICoords2D

  override def indexOpt: Option[Int] = None
}

/**
 * Контейнер параметров рендера css-стиля блока.
 * @param bf Экземпляр BlockField
 * @param offerN порядкой номер оффера в карточке.
 * @param yoff Сдвиг по оси y.
 * @param fid title либо descr обычно.
 */
case class FieldCssRenderArgs2(
  brArgs      : blk.IRenderArgs,
  aovf        : AOStringField,
  bf          : BfText,
  offerN      : Int,
  yoff        : Int,
  fid         : String,
  override val cssClasses: Seq[String] = Nil
) extends FieldCssRenderArgsT with IRenderArgsWrapper0 {

  override def xy: ICoords2D = Coords2D(38, 70*( offerN + 1) + yoff)
  override lazy val fieldCssClass: String = s"$fid-$offerN"

}
