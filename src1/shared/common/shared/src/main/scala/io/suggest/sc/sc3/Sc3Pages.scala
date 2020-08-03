package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.geo.MGeoPoint
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import japgolly.univeq._
import monocle.macros.GenLens

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
        (__ \ NODE_ID_FN).formatNullable[String] and
        (__ \ SEARCH_OPENED_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ GENERATION_FN).formatNullable[Long] and
        (__ \ TAG_NODE_ID_FN).formatNullable[String] and
        (__ \ LOC_ENV_FN).formatNullable[MGeoPoint] and
        (__ \ MENU_OPENED_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ FOCUSED_AD_ID_FN).formatNullable[String] and
        (__ \ FIRST_RUN_OPEN_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ DL_APP_OPEN_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ SETTINGS_OPEN_FN).formatNullable[Boolean].formatBooleanOrFalse and
        (__ \ SHOW_WELCOME_FN).formatNullable[Boolean].formatBooleanOrTrue and
        (__ \ VIRT_BEACONS_FN).formatNullable[Set[String]]
          .inmap[Set[String]](
            EmptyUtil.opt2ImplEmptyF(Set.empty),
            s => Option.when(s.nonEmpty)(s)
          )
      )(apply, unlift(unapply))
    }


    def nodeId = GenLens[MainScreen]( _.nodeId )
    def locEnv = GenLens[MainScreen]( _.locEnv )
    def searchOpened = GenLens[MainScreen]( _.searchOpened )
    def menuOpened = GenLens[MainScreen]( _.menuOpened )
    def showWelcome = GenLens[MainScreen]( _.showWelcome )


    implicit final class MainScreenOpsExt( private val mainScreen: MainScreen ) extends AnyVal {

      def isSamePlaceAs(ms2: MainScreen): Boolean = {
        (mainScreen.nodeId ==* ms2.nodeId) &&
        ((mainScreen.locEnv.isEmpty && ms2.locEnv.isEmpty) ||
         (mainScreen.locEnv.exists(gp1 => ms2.locEnv.exists(_ ~= gp1))))
      }

      /** Требуется ли гео-локация за неимением полезных данных? */
      def needGeoLoc: Boolean = {
        mainScreen.nodeId.isEmpty &&
        mainScreen.locEnv.isEmpty
      }

      /** Что-либо визуальное раскрыто на экране, помимо выдачи (панели, окошки, итд)? */
      def isSomeThingOpened: Boolean = {
        mainScreen.searchOpened ||
        mainScreen.menuOpened ||
        mainScreen.firstRunOpen ||
        mainScreen.dlAppOpen ||
        mainScreen.settingsOpen
      }

      /** Переключение в другое состояние с минимальными изменениями конфигурации интерфейса. */
      def silentSwitchingInto( mainScreen2: MainScreen ): MainScreen = {
        mainScreen2.copy(
          searchOpened  = mainScreen.searchOpened,
          menuOpened    = mainScreen.menuOpened,
          firstRunOpen  = mainScreen.firstRunOpen,
          showWelcome   = false,
          dlAppOpen     = mainScreen.dlAppOpen,
          virtBeacons     = Set.empty,
        )
      }

      /** Очень каноническое состояние выдачи без каких-либо уточнений. */
      def canonical: MainScreen = {
        mainScreen.copy(
          searchOpened      = false,
          menuOpened        = false,
          generation        = None,
          virtBeacons         = Set.empty,
        )
      }

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
                         dlAppOpen      : Boolean             = false,
                         settingsOpen   : Boolean             = false,
                         showWelcome    : Boolean             = true,
                         virtBeacons    : Set[String]         = Set.empty,
                       )
    extends Sc3Pages

}
