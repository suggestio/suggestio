package models.mcron

import scala.concurrent.duration.FiniteDuration


/** Интерфейс cron-задачи. */
trait ICronTask extends Runnable {
  def startDelay    : FiniteDuration
  def every         : FiniteDuration
  def displayName   : String
}

/**
 * Описание задача для Cron.
 * @param startDelay Задержка после старта перед первым исполнением задачи.
 * @param every Интервал повторения задачи.
 * @param displayName Отображаемое название задачи. Обычно, название вызываемого метода.
 * @param actionF Тело задачи.
 */
case class MCronTask(
  override val startDelay    : FiniteDuration,
  override val every         : FiniteDuration,
  override val displayName   : String
)(actionF: => Unit)
  extends ICronTask
{
  def run(): Unit = actionF
}
