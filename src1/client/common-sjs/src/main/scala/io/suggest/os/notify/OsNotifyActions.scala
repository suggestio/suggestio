package io.suggest.os.notify

import io.suggest.spa.DAction
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.2020 11:37
  * Description: Экшены контроллера системных уведомлений.
  */
trait IOsNotifyAction extends DAction

/** Запуск/остановка систем нотификации. */
case class NotifyStartStop(isStart: Boolean, maxTries: Int = 5) extends IOsNotifyAction
object NotifyStartStop {
  def maxTries = GenLens[NotifyStartStop](_.maxTries)
}

/** Показать указанные нотификации. */
case class ShowNotify( toasts: Seq[MOsToast] ) extends IOsNotifyAction

/** Скрыть указанные или просто все нотификации. */
case class CloseNotify( toastIds: Iterable[String] ) extends IOsNotifyAction

/** Запросить чтение пермишшена.
  *
  * @param onComplete Сборка эффекта-реакции.
  * @param isRequest true - это запрос, видимый пользователю.
  *                  false - это скрытая проверка текущего значения пермишена.
  */
case class NotifyPermission( isRequest: Boolean,
                             onComplete: Option[Option[Boolean] => DAction] = None ) extends IOsNotifyAction

