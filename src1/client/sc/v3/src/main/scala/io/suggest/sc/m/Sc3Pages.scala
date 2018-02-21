package io.suggest.sc.m

import io.suggest.geo.MGeoPoint
import io.suggest.sc.m.search.MSearchTab
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.17 16:03
  * Description: Модели роут для spa-роутера.
  */

/** Интерфейс-маркер для всех допустимых роут. */
sealed trait Sc3Pages


/** Реализации допустимых роут. */
object Sc3Pages {

  implicit def univEq: UnivEq[Sc3Pages] = UnivEq.derive


  object MainScreen {
    def empty = apply()
    implicit def univEq: UnivEq[MainScreen] = UnivEq.derive
  }

  /** Роута для основного экрана с какими-то доп.аргументами. */
  case class MainScreen(
                         nodeId         : Option[String]      = None,
                         searchOpened   : Boolean             = false,
                         searchTab      : Option[MSearchTab]  = None,
                         generation     : Option[Long]        = None,
                         tagNodeId      : Option[String]      = None,
                         locEnv         : Option[MGeoPoint]   = None,
                       )
    extends Sc3Pages
  {

    /** Требуется ли гео-локация за неимением полезных данных? */
    def needGeoLoc: Boolean = {
      nodeId.isEmpty && locEnv.isEmpty
    }

  }

}

