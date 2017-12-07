package io.suggest.sc.root.m

import io.suggest.sc.search.m.MSearchTab
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
                         generation     : Option[Long]        = None,
                         searchOpened   : Boolean             = false,
                         currSearchTab        : Option[MSearchTab]  = None
                         //geoPoint       : Option[MGeoPoint]   = None
                       )
    extends Sc3Pages

}

