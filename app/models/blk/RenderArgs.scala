package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 18:42
 * Description: Модель для параметров рендера блоков. Изначально жила в models.Stuff.
 */

object RenderArgs {

  /** Дефолтовый thread-safe инстанс параметров. Пригоден для рендера любой плитки блоков. */
  val DEFAULT = RenderArgs()

  val DOUBLE_SIZED_ARGS = RenderArgs(szMult = 2)
}

/**
 * Параметры рендера блока.
 * Всегда immutable класс!
 * @param withEdit Рендерим в редакторе.
 * @param isStandalone Рендерим блок как отдельную страницу? Отрабатывается через blocksBase.
 * @param szMult Мультипликатор размера (и относительных координат).
 * @param fullScreen Рендерим во весь рост. Такое бывает, когда юзер просматривает карточку.
 */
case class RenderArgs(
  withEdit      : Boolean = false,
  isStandalone  : Boolean = false,
  szMult        : Int = 1,
  fullScreen    : Boolean = false
) {

  @deprecated("Use szMult instead", "2014/oct/14")
  def canRenderDoubleSize = szMult == 2
}
