package io.suggest.ble.beaconer

import io.suggest.ble.IBleBeaconAction
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:09
  * Description: Сигналы для подписки на мониторинг маячков.
  */

/** Управление работой системой мониторинга маячков.
  *
  * @param isEnabled Новое состояние (вкл/выкл)
  * @param opts Опции, сохраняемые в состоянии beaconer'а.
  */
case class BtOnOff(isEnabled: Boolean,
                   opts: MBeaconerOpts = MBeaconerOpts.default) extends IBleBeaconAction

/** Экшен Результат подписки на события API. */
private[beaconer] case class HandleListenRes( listenTryRes: Try[IBleBeaconsApi] ) extends IBleBeaconAction

/** Сработал таймер ожидания для возможного уведомления всех страждущих. */
private[beaconer] case class MaybeNotifyAll(timestamp: Long) extends IBleBeaconAction


/** Экшен запуска сборки неактуальных маячков в состоянии. */
private[beaconer] case object DoGc extends IBleBeaconAction

/** Экшен, уведомляющий о завершении инициализации или деинициализации.
  *
  * @param tryEnabled Итоговое состояние, к которому пришла система.
  */
private[beaconer] case class BtOnOffFinish(tryEnabled: Try[Boolean] ) extends IBleBeaconAction
