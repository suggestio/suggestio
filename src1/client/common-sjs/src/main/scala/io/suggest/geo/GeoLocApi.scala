package io.suggest.geo

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.scalajs.js
import io.suggest.sjs.dom2._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.10.2020 10:32
  * Description: Универсальный интерфейс для абстрагирования различных API геолокации.
  */
trait GeoLocApi {

  def underlying: Option[js.Any]

  def isAvailable(): Boolean

  /** ОПРЕДЕЛИТЬ и наблюдать геолокацию. */
  def getAndWatchPosition(options: GeoLocApiWatchOptions ): Future[GeoLocWatchId_t]

  def clearWatch(watchId: GeoLocWatchId_t): Future[_]

}


/** Опции для абстрактного интерфейса [[GeoLocApi.getAndWatchPosition()]].
  */
final case class GeoLocApiWatchOptions(
                                        onLocation        : (MGeoLoc) => Unit,
                                        onError           : Option[PositionException => Unit] = None,
                                        watcher           : GeoLocWatcherInfo,
                                      )

/** Описания для инициализации watcher'а.
  *
  * @param highAccuracy Какие accuracy требуется получать?
  * @param maxAge Срок жизни.
  * @param watch Продолжительный мониторинг?
  */
final case class GeoLocWatcherInfo(
                                    watch             : Boolean,
                                    highAccuracy      : Option[Boolean]       = None,
                                    maxAge            : Option[Duration]      = None,
                                  )
