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
  * Используется stateful-инстанс из-за потребности абстрагироваться от двух слишком различных API (html5 api, cdb-bg-geoloc.).
  */
trait GeoLocApi {

  def reset(): Future[_]

  def configure(options: GeoLocApiWatchOptions): Future[_]

  def underlying: Option[js.Any]

  def isAvailable(): Boolean

  /** ОПРЕДЕЛИТЬ и наблюдать геолокацию. */
  def getAndWatchPosition(): Future[_]

  def getPosition(): Future[_]

  def clearWatch(): Future[_]

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

