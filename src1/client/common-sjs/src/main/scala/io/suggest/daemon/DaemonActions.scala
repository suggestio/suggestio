package io.suggest.daemon

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 15:05
  * Description: Общие экшены для контроллеров демонизации вёб-приложения.
  */
trait IDaemonAction extends DAction

/** Команда к инициализации или деиницилизации системы демонизации.
  *
  * @param initOpts Some() - инициализация с указанными параметрами.
  *             None - деинициализация и сворачивание работы.
  */
case class BgModeDaemonInit(initOpts: Option[MDaemonInitOpts] ) extends IDaemonAction

/** Команда к демонизации приложения. */
case class DaemonBgModeSet( isDaemon: Boolean ) extends IDaemonAction


trait IDaemonSleepAction extends DAction

/** Команда к запуску/остановке sleep-таймера.
  * @param options None - снять таймер.
  *                Some() - выставить таймер с указаннмыи параметрами.
  */
case class DaemonSleepTimerSet( options: Option[MDaemonSleepTimer] ) extends IDaemonSleepAction


/** Завершение работы таймера - обязательно для cordova-plugin-background-fetch. */
case object DaemonSleepTimerFinish extends IDaemonSleepAction


protected[daemon] case class DaemonSleepTimerUpdate( timerId: Option[Int] ) extends IDaemonSleepAction