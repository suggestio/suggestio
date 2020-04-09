package io.suggest.os.notify.api.cnl

import cordova.plugins.notification.local.{CnlEventData, CnlToast}
import io.suggest.os.notify.{IOsNotifyAction, MOsToast}

import scala.collection.immutable.HashMap
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.2020 0:18
  * Description: Внутренние экшены для cordova-notification-local.
  */
sealed trait ICnlAction extends IOsNotifyAction

/** Изменение конфигурации листенеров. */
case class UpdateCnlListeners( addListeners: HashMap[String, js.Function], removeListeners: Set[String] ) extends ICnlAction

/** Обработать событие от cordova-плагина. */
case class HandleCnlEvent( data: CnlEventData, toast: Option[CnlToast] = None ) extends ICnlAction


/** Контейнер инфы по одной нотификации. */
case class CnlToastInfo( intId: Int, osToast: MOsToast, cnlToast: CnlToast ) extends ICnlAction

/** Экшен об успешном размещение указанных нотификации. */
case class SetCnlToasts( toasts: Iterable[CnlToastInfo], append: Boolean ) extends ICnlAction

/** Удалить указанные toast'ы. */
case class RmCnlToasts( toastIds: Iterable[String], intIds: Iterable[Int] ) extends ICnlAction

case class CnlSavePermission(isAllowedOpt: Option[Boolean] ) extends ICnlAction
