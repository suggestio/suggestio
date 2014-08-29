package io.suggest.model.geo

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 15:12
 * Description: Хелперы для упрощения написания тестов для geo-моделей. Используются для построения
 * произвольных гео-объектов на рандомных планетарных координатах.
 */

trait CoordRnd {

  protected val rnd = new Random()

  protected def newDouble = rnd.nextDouble()
  protected def newLat = (newDouble - 0.5) * 180      // Долгота: север -90 .. 90 юг
  protected def newLon = (newDouble - 0.5) * 360      // Широта: запад -180 .. 180 восток

}


trait LatLonRnd[T] extends CoordRnd {

  protected val testPerTry = 100

  protected def mkInstance: T

  protected def mkTests(f: T => Unit): Unit = {
    (1 to testPerTry) foreach { i =>
      val v = mkInstance
      f(v)
    }
  }

}


trait CoordLineRnd extends CoordRnd {

  val minCoordLineLen = 50
  val coordLineLenRnd = 200

  protected def rndCoordRow: Seq[GeoPoint] = {
    val len = rnd.nextInt(coordLineLenRnd) + minCoordLineLen
    (0 to len).map { j =>
      GeoPoint(lat = newLat, lon = newLon)
    }
  }

}

