package models.blk

import io.suggest.ym.model.common.MImgSizeT
import models._
import models.im.MImg
import play.api.mvc.Call
import util.img.DynImgUtil

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
 * @param isStandalone Рендерим блок как отдельную страницу? Отрабатывается через blocksBase.
 * @param szMult Мультипликатор размеров карточки.
 * @param wideBg Рендерим бэкграунд на широкую. Если у карточки разрешен просмотр на широкую, то фон будет отрендерен
 *               вне блока, широким, а тело блока сдвинуто согласно кропу.
 * @param inlineStyles Рендерить стили инлайново?
 * @param withCssClasses Дополнительные css-классы, которые относятся к рендеру.
 */
case class RenderArgs(
  szMult        : SzMult_t,
  withEdit      : Boolean = false,
  isStandalone  : Boolean = false,
  wideBg        : Option[WideBgRenderCtx] = None,
  inlineStyles  : Boolean = true,
  withCssClasses: Seq[String] = Nil
)


/**
 * При рендере bg по-широкому в шаблоне нужны данные по этой широкой картинке.
 * Эти параметры приходят из контроллера, т.к. для их сборки требуется асинхронный контекст (ибо работа с кассандрой).
 * @param height Высота картинки.
 * @param width Ширина картинки.
 */
case class WideBgRenderCtx(
  height        : Int,
  width         : Int,
  dynCallArgs   : MImg
) extends MImgSizeT {

  def dynImgCall: Call = DynImgUtil.imgCall(dynCallArgs)

}


/** Параметры для рендера внешнего css блока. */
trait CssRenderArgsT {
  def mad       : MAdT
  def szMult    : SzMult_t
  def cssClasses: Seq[String]
}
case class CssRenderArgs(mad: MAdT, szMult: SzMult_t, cssClasses: Seq[String] = Nil)
  extends CssRenderArgsT


/** Интерфейс аргументов шаблона _blockStyleCss для рендера стиля блока. */
trait FieldCssRenderArgsT extends CssRenderArgsT {
  def aovf          : AOValueField
  def bf            : BlockAOValueFieldT
  def fieldCssClass : String
  def xy            : ICoords2D
}

case class FieldCssRenderArgs(
  szMult          : SzMult_t,
  mad             : MAdT,
  aovf            : AOValueField,
  bf              : BlockAOValueFieldT,
  fieldCssClass   : String,
  xy              : ICoords2D,
  cssClasses      : Seq[String] = Nil
) extends FieldCssRenderArgsT

/**
 * Контейнер параметров рендера css-стиля блока.
 * @param mad рекламная карточка.
 * @param bf Экземпляр BlockField
 * @param offerN порядкой номер оффера в карточке.
 * @param yoff Сдвиг по оси y.
 * @param fid title либо descr обычно.
 */
case class FieldCssRenderArgs2(
  szMult  : SzMult_t,
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
