package io.suggest.os.notify.api.html5

import io.suggest.spa.DAction

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.2020 15:37
  * Description: Экшены для html5-контроллера нотификаций.
  *
  */
sealed trait H5nActions extends DAction


/** Закэшировать результат проверки в состоянии. */
private case class H5nSavePermFx(perm: Option[Boolean] ) extends DAction

/** Внутренний экшен обновления списка нотификаций в состоянии. */
private case class H5nAddNotifications( nots: HashMap[String, MH5nToastInfo] ) extends DAction

/** Удалить нотификацию. */
private case class H5nRemoveNotifications( toastIds: Iterable[String] ) extends DAction
