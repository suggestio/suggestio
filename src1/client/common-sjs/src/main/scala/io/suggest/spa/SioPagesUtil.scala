package io.suggest.spa

import io.suggest.common.empty.OptionUtil._
import io.suggest.geo.MGeoPoint
import io.suggest.id.login.MLoginTabs
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.ScConstants
import io.suggest.text.UrlUtilJs
import japgolly.univeq._

import scala.scalajs.js.URIUtils
import scala.util.Try

object SioPagesUtil extends Log {

  /** Parse Sc3 state from URL query string.
    *
    * @param qs Showcase URL QueryString.
    * @return Parsed Sc3 state.
    */
  def parseSc3FromQs(qs: String): SioPages.Sc3 = {
    val tokens = qs
      .split('&')
      .iterator
      .flatMap { kvStr =>
        if (kvStr.isEmpty) {
          Nil
        } else {
          kvStr.split('=') match {
            case arr if arr.length ==* 2 =>
              val arr2 = arr.iterator
                .map(URIUtils.decodeURIComponent)
              val k2 = arr2.next()
              val v2 = arr2.next()
              (k2 -> v2) :: Nil

            case other =>
              logger.warn( ErrorMsgs.SC_URL_HASH_UNKNOWN_TOKEN, msg = other )
              Nil
          }
        }
      }
      .toMap

    parseSc3FromQsTokens( tokens )
  }
  def parseSc3FromQsTokens(tokens: collection.Map[String, String]): SioPages.Sc3 = {
    // TODO This logic (below) duplicates MainScreen.FORMAT.

    def _boolOptTok(key: String): Option[Boolean] = {
      tokens
        .get( key )
        .flatMap { boolStr =>
          Try(boolStr.toBoolean).toOption
        }
    }
    def _boolOrFalseTok(key: String): Boolean =
      _boolOptTok(key).getOrElseFalse

    val K = ScConstants.ScJsState
    SioPages.Sc3(
      nodeId = tokens.get( K.NODE_ID_FN ),
      searchOpened = _boolOrFalseTok( K.SEARCH_OPENED_FN ),
      generation = tokens.get( K.GENERATION_FN )
        .flatMap( MGen.parse ),
      tagNodeId = tokens.get( K.TAG_NODE_ID_FN ),
      locEnv = tokens.get( K.LOC_ENV_FN )
        .flatMap( MGeoPoint.fromString ),
      menuOpened = _boolOrFalseTok( K.MENU_OPENED_FN ),
      focusedAdId = tokens.get( K.FOCUSED_AD_ID_FN ),
      firstRunOpen = _boolOrFalseTok( K.FIRST_RUN_OPEN_FN ),
      dlAppOpen = _boolOrFalseTok( K.DL_APP_OPEN_FN ),
      settingsOpen = _boolOrFalseTok( K.SETTINGS_OPEN_FN ),
      showWelcome = _boolOptTok( K.SHOW_WELCOME_FN ).getOrElseTrue,
      virtBeacons = tokens.view
        .filterKeys(_ startsWith K.VIRT_BEACONS_FN)
        .valuesIterator
        .toSet,
      login = for {
        currTabIdRaw <- tokens.get( K.LOGIN_FN )
        currTabId <- Try( currTabIdRaw.toInt ).toOption
        currTab   <- MLoginTabs.withValueOpt( currTabId )
      } yield {
        SioPages.Login( currTab )
      },
    )
  }


  /** Serialize Sc3-route back into queryString.
    *
    * @param mainScreen Showcase state.
    * @return URL query string.
    */
  def sc3ToQs(mainScreen: SioPages.Sc3): String = {
    val K = ScConstants.ScJsState

    var acc: List[(String, String)] = Nil

    // TODO Logic duplicates MainScreen.FORMAT + jsRouter._o2qs(). Maybe to unify it somehow?

    // By now, generation is serialized into URL. Maybe, disable it to resolve iOS Webapp retries/problems.
    for (gen <- mainScreen.generation)
      acc ::= K.GENERATION_FN -> MGen.serialize(gen)

    for (geoLoc <- mainScreen.locEnv)
      acc ::= K.LOC_ENV_FN -> geoLoc.toString

    for (nodeId <- mainScreen.nodeId)
      acc ::= K.NODE_ID_FN -> nodeId

    // Tags info:
    for (tagNodeId <- mainScreen.tagNodeId) {
      acc ::= K.TAG_NODE_ID_FN -> tagNodeId
      // TODO  TAG_FACE_FN -> tagInfo.face
    }

    // Focused ad id, if any:
    for (focusedAdId <- mainScreen.focusedAdId) yield {
      acc ::= K.FOCUSED_AD_ID_FN -> focusedAdId
    }

    // Right search panel state.
    if (mainScreen.searchOpened)
      acc ::= K.SEARCH_OPENED_FN -> mainScreen.searchOpened.toString

    // Left menu panel, if opened:
    if (mainScreen.menuOpened)
      acc ::= K.MENU_OPENED_FN -> mainScreen.menuOpened.toString

    // First run dialog.
    if (mainScreen.firstRunOpen)
      acc ::= K.FIRST_RUN_OPEN_FN -> mainScreen.firstRunOpen.toString

    if (mainScreen.dlAppOpen)
      acc ::= K.DL_APP_OPEN_FN -> mainScreen.dlAppOpen.toString

    if (mainScreen.settingsOpen)
      acc ::= K.SETTINGS_OPEN_FN -> mainScreen.settingsOpen.toString

    if (!mainScreen.showWelcome)
      acc ::= K.SHOW_WELCOME_FN -> mainScreen.showWelcome.toString

    for {
      (bcnId, i) <- mainScreen.virtBeacons.iterator.zipWithIndex
    }
      acc ::= s"${K.VIRT_BEACONS_FN}[$i]" -> bcnId

    for (login <- mainScreen.login)
      acc ::= K.LOGIN_FN -> login.currTab.value.toString

    UrlUtilJs.qsPairsToString(acc)
  }

}
