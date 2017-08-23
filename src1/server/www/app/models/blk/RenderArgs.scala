package models.blk

import io.suggest.ad.blk.ent.MEntity
import io.suggest.common.css.ITopLeft
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.model.n2.node.MNode
import models.{BfText, BlockConf, blk}
import models.im.make.{IMakeResult, MakeResult}
import models.msc.{IScApiVsn, MScApiVsn, MScApiVsns}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 18:42
 * Description: Модель для параметров рендера блоков. Изначально жила в models.Stuff.
 */

trait IRenderArgs extends IScApiVsn {

  /** Блок. */
  def bc              : BlockConf

  /** Рекламная карточка, которую надо отрендерить. */
  def mad             : MNode

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
  override def apiVsn : MScApiVsn = MScApiVsns.unknownVsn

  /** Порядковый номер. Заполняется только для плитки выдачи. */
  def indexOpt        : Option[Int]

  /** Рендер для разглядывания блока, а не в плитке. */
  def isFocused       : Boolean

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

  override def bc           = brArgs.bc
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
  override val mad            : MNode,
  override val szMult         : SzMult_t,
  override val bc             : BlockConf,
  override val bgImg          : Option[MakeResult],
  override val isFocused      : Boolean                 = false,
  override val withEdit       : Boolean                 = false,
  override val inlineStyles   : Boolean                 = true,
  override val cssClasses     : Seq[String]             = Nil,
  override val topLeft        : Option[ITopLeft]        = None,
  override val indexOpt       : Option[Int]             = None,
  override val apiVsn         : MScApiVsn               = MScApiVsns.unknownVsn
)
  extends IRenderArgs




/**
 * Контейнер параметров рендера css-стиля блока.
 * @param bf Экземпляр BlockField
 * @param entity Текущее поле карточки.
 * @param yoff Сдвиг по оси y.
 */
case class FieldCssRenderArgs(
                               brArgs      : blk.IRenderArgs,
                               bf          : BfText,
                               entity      : MEntity,
                               yoff        : Int,
                               override val isFocused : Boolean     = false,
                               override val cssClasses: Seq[String] = Nil
)
  extends IRenderArgsWrapper0
{
  override def indexOpt: Option[Int] = None

  def aovfCoords = entity.coords

  def xy = MCoords2di(38, 70*( entity.id + 1) + yoff)

}
