package io.suggest.spa

import io.suggest.common.empty.EmptyUtil
import io.suggest.geo.MGeoPoint
import io.suggest.id.login.{MLoginTab, MLoginTabs}
import japgolly.univeq.{UnivEq, _}
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.17 16:03
  * Description: Модели роут для spa-роутера.
  */

/** Интерфейс-маркер для всех допустимых роут. */
sealed trait SioPages


/** Реализации допустимых роут. */
object SioPages {

  @inline implicit def univEq: UnivEq[SioPages] = UnivEq.derive


  object Sc3 {

    def empty = apply()

    @inline implicit def univEq: UnivEq[Sc3] = UnivEq.derive

    /** Поддержка play-json для qs-модели нужна для эксплуатации _o2qs() в jsRouter'е. */
    implicit def scJson: OFormat[Sc3] = {
      import io.suggest.common.empty.OptionUtil._
      import io.suggest.sc.ScConstants.ScJsState._
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
          ) and
        (__ \ Login.Fields.CURR_TAB_FN).formatNullable[MLoginTab]
          .inmap[Option[Login]]( _.map(Login(_)), _.map(_.currTab) )
      )(apply, unlift(unapply))
    }


    def nodeId = GenLens[Sc3]( _.nodeId )
    def locEnv = GenLens[Sc3]( _.locEnv )
    def searchOpened = GenLens[Sc3]( _.searchOpened )
    def menuOpened = GenLens[Sc3]( _.menuOpened )
    def showWelcome = GenLens[Sc3]( _.showWelcome )
    def virtBeacons = GenLens[Sc3]( _.virtBeacons )
    def login = GenLens[Sc3]( _.login )


    implicit final class MainScreenOpsExt( private val mainScreen: Sc3 ) extends AnyVal {

      def isSamePlaceAs(ms2: Sc3): Boolean = {
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
      def silentSwitchingInto( mainScreen2: Sc3 ): Sc3 = {
        mainScreen2.copy(
          searchOpened  = mainScreen.searchOpened,
          menuOpened    = mainScreen.menuOpened,
          firstRunOpen  = mainScreen.firstRunOpen,
          showWelcome   = false,
          dlAppOpen     = mainScreen.dlAppOpen,
          virtBeacons   = Set.empty,
        )
      }

      /** Очень каноническое состояние выдачи без каких-либо уточнений. */
      def canonical: Sc3 = {
        mainScreen.copy(
          searchOpened      = false,
          menuOpened        = false,
          generation        = None,
          virtBeacons       = Set.empty,
        )
      }

    }

  }

  /** Роута для основного экрана с какими-то доп.аргументами. */
  case class Sc3(
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
                  login          : Option[SioPages.Login]   = None,
                )
    extends SioPages




  /** Класс-контейнер данных URL текущей формы.
    *
    * @param currTab Текущий открытый таб.
    * @param returnUrl Значение "?r=..." в ссылке.
    */
  final case class Login(
                          currTab       : MLoginTab       = MLoginTabs.default,
                          returnUrl     : Option[String]  = None,
                        )
    extends SioPages
    with DAction

  object Login {

    def default = apply()

    object Fields {
      def CURR_TAB_FN   = "t"
      def RETURN_URL_FN = "r"
    }

    def currTab = GenLens[Login](_.currTab)
    def returnUrl = GenLens[Login](_.returnUrl)

  }

}
