package models.mcron

import scala.concurrent.duration.FiniteDuration

/**
 * Описание задача для Cron.
 * @param startDelay Задержка после старта перед первым исполнением задачи.
 * @param every Интервал повторения задачи.
 * @param displayName Отображаемое название задачи. Обычно, название вызываемого метода.
  *                    Должно быть уникально в рамках одного контейнера.
 */
case class MCronTask(
                      startDelay    : FiniteDuration,
                      every         : FiniteDuration,
                      displayName   : String,
                    )( val run: () => Unit )
