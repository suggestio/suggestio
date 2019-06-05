package models.mcron

import scala.concurrent.duration.FiniteDuration

/**
 * Описание задача для Cron.
 * @param startDelay Задержка после старта перед первым исполнением задачи.
 * @param every Интервал повторения задачи.
 * @param displayName Отображаемое название задачи. Обычно, название вызываемого метода.
 * @param actionF Тело задачи.
 */
case class MCronTask(
                      startDelay    : FiniteDuration,
                      every         : FiniteDuration,
                      displayName   : String
                    )(actionF: => Unit)
{
  def run(): Unit = actionF
}
