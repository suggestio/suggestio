package io.suggest.daemon

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 15:05
  * Description: Общие экшены для контроллеров демонизации вёб-приложения.
  */
sealed trait IDaemonAction extends DAction


/** Команда к инициализации или деиницилизации системы демонизации.
  *
  * @param initOpts Some() - инициализация с указанными параметрами.
  *             None - деинициализация и сворачивание работы.
  */
case class DaemonizerInit( initOpts: Option[MDaemonInitOpts] ) extends IDaemonAction


/** Команда к демонизации приложения. */
case class Daemonize( isDaemon: Boolean ) extends IDaemonAction


/** Команда к запуску/остановке sleep-таймера.
  * @param options None - снять таймер.
  *                Some() - выставить таймер с указаннмыи параметрами.
  */
case class DaemonSleepTimerSet( options: Option[MDaemonSleepTimer] ) extends IDaemonAction


/** Завершение работы таймера - обязательно для cordova-plugin-background-fetch. */
case object DaemonSleepTimerFinish extends IDaemonAction


protected[daemon] case class DaemonSleepTimerUpdate( timerId: Option[Int] ) extends IDaemonAction