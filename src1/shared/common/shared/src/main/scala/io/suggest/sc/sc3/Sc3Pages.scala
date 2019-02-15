package io.suggest.sc.sc3

import io.suggest.geo.MGeoPoint
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  @inline implicit def univEq: UnivEq[Sc3Pages] = UnivEq.derive


  object MainScreen {

    def empty = apply()

    @inline implicit def univEq: UnivEq[MainScreen] = UnivEq.derive

    /** Поддержка play-json для qs-модели нужна для эксплуатации _o2qs() в jsRouter'е. */
    implicit def MAIN_SCREEN_FORMAT: OFormat[MainScreen] = {
      import io.suggest.sc.ScConstants.ScJsState._
      import io.suggest.common.empty.OptionUtil._
      import MGeoPoint.JsonFormatters.{PIPE_DELIM_STRING_READS, PIPE_DELIM_STRING_WRITES}
      (
        (__ \ ADN_ID_FN).formatNullable[String] and
        (__ \ CAT_SCR_OPENED_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ GENERATION_FN).formatNullable[Long] and
        (__ \ TAG_NODE_ID_FN).formatNullable[String] and
        (__ \ LOC_ENV_FN).formatNullable[MGeoPoint] and
        (__ \ GEO_SCR_OPENED_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ FOCUSED_AD_ID_FN).formatNullable[String] and
        (__ \ FIRST_RUN_OPEN_FN).formatNullable[Boolean].formatBooleanOrFalse
      )(apply, unlift(unapply))
    }

  }

  /** Роута для основного экрана с какими-то доп.аргументами. */
  case class MainScreen(
                         nodeId         : Option[String]      = None,
                         searchOpened   : Boolean             = false,
                         generation     : Option[Long]        = None,
                         tagNodeId      : Option[String]      = None,
                         locEnv         : Option[MGeoPoint]   = None,
                         menuOpened     : Boolean             = false,
                         focusedAdId    : Option[String]      = None,
                         firstRunOpen   : Boolean             = false,
                       )
    extends Sc3Pages
  {

    /** Требуется ли гео-локация за неимением полезных данных? */
    def needGeoLoc: Boolean = {
      nodeId.isEmpty && locEnv.isEmpty
    }

  }

}

