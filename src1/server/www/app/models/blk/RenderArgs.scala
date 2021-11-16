package models.blk

import io.suggest.common.css.ITopLeft
import io.suggest.n2.node.MNode
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import models.im.make.MakeResult

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 18:42
 * Description: Модель для параметров рендера блоков. Изначально жила в models.Stuff.
 */

trait IRenderArgs {

  /** Рекламная карточка, которую надо отрендерить. */
  def mad             : MNode

  /** Мультипликатор размеров карточки. */
  def szMult          : SzMult_t

  /** Данные о фоновой картинке, если есть. */
  def bgImg           : Option[MakeResult]

  /** Рендерим в редакторе. */
  def withEdit        : Boolean

  /** Рендерить стили инлайново? */
  def inlineStyles    : Boolean

  /** Дополнительные css-классы, которые относятся к рендеру. */
  def cssClasses      : Seq[String]

  /** Стили для div .sm-block. */
  def topLeft         : Option[ITopLeft]

  /** Версия API выдачи. */
  def apiVsn : MScApiVsn = MScApiVsns.unknownVsn

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
  final lazy val wideBg: Option[MakeResult] = {
    bgImg.filter(_.isWide)
  }

}


/** Параметры рендера блока. Дефолтовая реализация [[IRenderArgs]]. */
case class RenderArgs(
  override val mad            : MNode,
  override val szMult         : SzMult_t,
                     // TODO Удалить bgImg и возможно ещё какие-то неактуальные поля.
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
