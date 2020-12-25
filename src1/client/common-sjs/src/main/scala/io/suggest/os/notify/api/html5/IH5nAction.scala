package io.suggest.os.notify.api.html5

import io.suggest.os.notify.IOsNotifyAction

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.2020 15:37
  * Description: Экшены для html5-контроллера нотификаций.
  *
  */
sealed trait IH5nAction extends IOsNotifyAction


/** Закэшировать результат проверки в состоянии. */
private case class H5nSavePermFx(perm: Option[Boolean] ) extends IH5nAction

/** Внутренний экшен обновления списка нотификаций в состоянии. */
private case class H5nAddNotifications( nots: HashMap[String, MH5nToastInfo] ) extends IH5nAction

/** Удалить нотификацию. */
private case class H5nRemoveNotifications( toastIds: Iterable[String] ) extends IH5nAction
