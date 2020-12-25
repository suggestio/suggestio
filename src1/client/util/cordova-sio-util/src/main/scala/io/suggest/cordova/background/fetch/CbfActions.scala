package io.suggest.cordova.background.fetch

import io.suggest.daemon.IDaemonSleepAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 18:05
  */
sealed trait ICbfAction extends IDaemonSleepAction

/** Срабатывание callback'а для указанного id. */
case class CbfTaskCall(taskId: String) extends ICbfAction

case class CbfConfigFail( ex: Throwable ) extends ICbfAction

/** Смена isEnabled-состояния. */
case class CbfSetEnabled( isEnabledTry: Try[Boolean] ) extends ICbfAction
