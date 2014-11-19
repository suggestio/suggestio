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
  val DEFAULT = RenderArgs()

  def DOUBLE_SIZED_ARGS = RenderArgs(szMult = 2F)
}

/**
 * Параметры рендера блока.
 * Всегда immutable класс!
 * @param withEdit Рендерим в редакторе.
 * @param isStandalone Рендерим блок как отдельную страницу? Отрабатывается через blocksBase.
 * @param szMult Мультипликатор размера (и относительных координат).
 * @param wideBg Рендерим бэкграунд на широкую. Если у карточки разрешен просмотр на широкую, то фон будет отрендерен
 *               вне блока, широким, а тело блока сдвинуто согласно кропу.
 */
case class RenderArgs(
  withEdit      : Boolean = false,
  isStandalone  : Boolean = false,
  szMult        : Float = 1.0F,
  wideBg        : Option[WideBgRenderCtx] = None
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


/** Интерфейс аргументов шаблона _blockStyleCss для рендера стиля блока. */
trait CssRenderArgsT {
  def madId     : Option[String]
  def aovf      : AOValueField
  def bf        : BlockAOValueFieldT
  def szMult    : Float
  def cssClass  : String
  def xy        : ICoords2D
}

case class CssRenderArgs(
  madId     : Option[String],
  aovf      : AOValueField,
  bf        : BlockAOValueFieldT,
  szMult    : Float,
  cssClass  : String,
  xy        : ICoords2D
) extends CssRenderArgsT

/**
 * Контейнер параметров рендера css-стиля блока.
 * @param madId id рекламной карточки, если есть.
 * @param bf Экземпляр BlockField
 * @param offerN порядкой номер оффера в карточке.
 * @param yoff Сдвиг по оси y.
 * @param fid title либо descr обычно.
 */
case class CssRenderArgs2(
  madId   : Option[String],
  aovf    : AOStringField,
  bf      : BfText,
  offerN  : Int,
  yoff    : Int,
  szMult  : Float,
  fid     : String
) extends CssRenderArgsT {
  override def xy: ICoords2D = Coords2D(38, 70*( offerN + 1) + yoff)
  override val cssClass: String = s"$fid-$offerN"
}

