package models.msc

import io.suggest.primo.IUnderlying

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 10:15
 * Description: Интерфейсы и врапперы для цветов.
 */
/** Интерфейс цвета переднего плана. */
trait IFgColor {
  /** Цвет элементов переднего плана. */
  def fgColor: String
}
/** Враппер для интерфейса цвета переднего плана. */
trait IFgColorWrapper extends IFgColor with IUnderlying {
  override def _underlying: IFgColor
  override def fgColor = _underlying.fgColor
}


/** Интерфейс цвета заднего плана. */
trait IBgColor {
  /** Цвет фона выдачи. */
  def bgColor: String
}
/** Враппер над интерфейсом цвета заднего плана. */
trait IBgColorWrapper extends IBgColor with IUnderlying {
  override def _underlying: IBgColor
  override def bgColor = _underlying.bgColor
}


/** Общий интерфейс цветов выдачи. */
trait IColors extends IBgColor with IFgColor
/** Враппер над общим интерфейсом цветов выдачи. */
trait IColorsWrapper extends IColors with IBgColorWrapper with IFgColorWrapper {
  override def _underlying: IColors
}

/** Дефолтовая реализация модели [[IColors]]. */
case class Colors(bgColor: String, fgColor: String) extends IColors
