package io.suggest.daemon.cordova

import io.suggest.spa.DAction

import scala.collection.immutable.HashMap
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 14:56
  * Description: Экшены для контроллера плагина c-p-bg-mode.
  */
sealed trait ICbgmAction extends DAction

/** Пришло событие от плагина. */
final case class CbgmEvent( eventType: String ) extends ICbgmAction

/** Обновление списка листенеров в состоянии. */
final case class CbgmUpdateListeners( add: HashMap[String, js.Function], remove: Iterable[String] ) extends ICbgmAction
