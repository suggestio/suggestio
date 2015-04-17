package models.blk

import models._
import models.im.make.IMakeResult

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 18:42
 * Description: Модель для параметров рендера блоков. Изначально жила в models.Stuff.
 */

object RenderArgs {

  /** Дефолтовый thread-safe инстанс параметров. Пригоден для рендера любой плитки блоков. */
  val DEFAULT = RenderArgs(szMult = 1.0F)

  def DOUBLE_SIZED_ARGS = RenderArgs(szMult = 2F)
}

/**
 * Параметры рендера блока.
 * Всегда immutable класс!
 * @param withEdit Рендерим в редакторе.
 * @param szMult Мультипликатор размеров карточки.
 * @param wideBg Рендерим бэкграунд на широкую. Если у карточки разрешен просмотр на широкую, то фон будет отрендерен
 *               вне блока, широким, а тело блока сдвинуто согласно кропу.
 * @param inlineStyles Рендерить стили инлайново?
 * @param withCssClasses Дополнительные css-классы, которые относятся к рендеру.
 * @param blockStyle Стили для div .sm-block.
 */
case class RenderArgs(
  szMult          : SzMult_t,
  withEdit        : Boolean                 = false,
  wideBg          : Option[IMakeResult]     = None,
  inlineStyles    : Boolean                 = true,
  withCssClasses  : Seq[String]             = Nil,
  blockStyle      : Option[String]          = None
)


/** Параметры для рендера внешнего css блока. */
trait CssRenderArgsT {
  def mad         : MAdT
  def brArgs      : blk.RenderArgs
  def cssClasses  : Seq[String]
}
object CssRenderArgs {
  def apply(mad: MAdT, brArgs: blk.RenderArgs): CssRenderArgs = {
    apply(mad, brArgs, brArgs.withCssClasses)
  }
}
case class CssRenderArgs(mad: MAdT, brArgs: blk.RenderArgs, cssClasses: Seq[String])
  extends CssRenderArgsT


/** Интерфейс аргументов шаблона _blockStyleCss для рендера стиля блока. */
trait FieldCssRenderArgsT extends CssRenderArgsT {
  def aovf          : AOValueField
  def bf            : BlockAOValueFieldT
  def fieldCssClass : String
  def xy            : ICoords2D
}

/**
 * Контейнер параметров рендера css-стиля блока.
 * @param mad рекламная карточка.
 * @param bf Экземпляр BlockField
 * @param offerN порядкой номер оффера в карточке.
 * @param yoff Сдвиг по оси y.
 * @param fid title либо descr обычно.
 */
case class FieldCssRenderArgs2(
  brArgs  : blk.RenderArgs,
  mad     : MAdT,
  aovf    : AOStringField,
  bf      : BfText,
  offerN  : Int,
  yoff    : Int,
  fid     : String,
  cssClasses: Seq[String] = Nil
) extends FieldCssRenderArgsT {

  override def xy: ICoords2D = Coords2D(38, 70*( offerN + 1) + yoff)
  override val fieldCssClass: String = s"$fid-$offerN"

}
