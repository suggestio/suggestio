package com.materialui

import japgolly.scalajs.react.component.Js.{RawMounted, UnmountedWithRawType}
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent, ReactMouseEventFromHtml}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object MuiSvgIcon {
  implicit class SvgIconApply(icon: MuiSvgIcon) {
    def apply(svgProps: MuiSvgIconProps = new MuiSvgIconProps {})(children: VdomNode*): UnmountedWithRawType[MuiSvgIconProps, Null, RawMounted[MuiSvgIconProps,Null]] = {
      val Component = JsComponent[MuiSvgIconProps, Children.Varargs, Null](icon)
      Component(svgProps)(children: _*)
    }
  }
}

trait MuiSvgIconProps extends js.Object {
  val key: js.UndefOr[String] = js.undefined
  val ref: js.UndefOr[String] = js.undefined
  val color: js.UndefOr[MuiColor] = js.undefined
  val hoverColor: js.UndefOr[MuiColor] = js.undefined
  val onMouseEnter: js.UndefOr[ReactMouseEventFromHtml => Unit] = js.undefined
  val onMouseLeave: js.UndefOr[ReactMouseEventFromHtml => Unit] = js.undefined
  val style: js.UndefOr[js.Any] = js.undefined
  val viewBox: js.UndefOr[String] = js.undefined
  val className: js.UndefOr[String] = js.undefined
  val htmlColor: js.UndefOr[String] = js.undefined
}

@js.native
trait MuiSvgIcon extends js.Any

object MuiSvgIcons {
  // TODO Fix all paths
  @js.native @JSImport("@material-ui/icons/action/accessibility", JSImport.Default)
  object ActionAccessibility extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/accessible", JSImport.Default)
  object ActionAccessible extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/account-balance-wallet", JSImport.Default)
  object ActionAccountBalanceWallet extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/account-balance", JSImport.Default)
  object ActionAccountBalance extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/account-box", JSImport.Default)
  object ActionAccountBox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/account-circle", JSImport.Default)
  object ActionAccountCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/add-shopping-cart", JSImport.Default)
  object ActionAddShoppingCart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/alarm-add", JSImport.Default)
  object ActionAlarmAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/alarm-off", JSImport.Default)
  object ActionAlarmOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/alarm-on", JSImport.Default)
  object ActionAlarmOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/alarm", JSImport.Default)
  object ActionAlarm extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/all-out", JSImport.Default)
  object ActionAllOut extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/android", JSImport.Default)
  object ActionAndroid extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/announcement", JSImport.Default)
  object ActionAnnouncement extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/aspect-ratio", JSImport.Default)
  object ActionAspectRatio extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assessment", JSImport.Default)
  object ActionAssessment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment-ind", JSImport.Default)
  object ActionAssignmentInd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment-late", JSImport.Default)
  object ActionAssignmentLate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment-return", JSImport.Default)
  object ActionAssignmentReturn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment-returned", JSImport.Default)
  object ActionAssignmentReturned extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment-turned-in", JSImport.Default)
  object ActionAssignmentTurnedIn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/assignment", JSImport.Default)
  object ActionAssignment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/autorenew", JSImport.Default)
  object ActionAutorenew extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/backup", JSImport.Default)
  object ActionBackup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/book", JSImport.Default)
  object ActionBook extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/bookmark-border", JSImport.Default)
  object ActionBookmarkBorder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/bookmark", JSImport.Default)
  object ActionBookmark extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/bug-report", JSImport.Default)
  object ActionBugReport extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Build", JSImport.Default)
  object Build extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Cached", JSImport.Default)
  object Cached extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/camera-enhance", JSImport.Default)
  object ActionCameraEnhance extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/card-giftcard", JSImport.Default)
  object ActionCardGiftcard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/card-membership", JSImport.Default)
  object ActionCardMembership extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/card-travel", JSImport.Default)
  object ActionCardTravel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/change-history", JSImport.Default)
  object ActionChangeHistory extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/CheckCircle", JSImport.Default)
  object CheckCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/chrome-reader-mode", JSImport.Default)
  object ActionChromeReaderMode extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/class", JSImport.Default)
  object ActionClass extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/code", JSImport.Default)
  object ActionCode extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/compare-arrows", JSImport.Default)
  object ActionCompareArrows extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/copyright", JSImport.Default)
  object ActionCopyright extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/credit-card", JSImport.Default)
  object ActionCreditCard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/dashboard", JSImport.Default)
  object ActionDashboard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/date-range", JSImport.Default)
  object ActionDateRange extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/DeleteForever", JSImport.Default)
  object DeleteForever extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Delete", JSImport.Default)
  object Delete extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/description", JSImport.Default)
  object ActionDescription extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/dns", JSImport.Default)
  object ActionDns extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/DoneAll", JSImport.Default)
  object DoneAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Done", JSImport.Default)
  object Done extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/DoneOutline", JSImport.Default)
  object DoneOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/donut-large", JSImport.Default)
  object ActionDonutLarge extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/donut-small", JSImport.Default)
  object ActionDonutSmall extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/eject", JSImport.Default)
  object ActionEject extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/euro-symbol", JSImport.Default)
  object ActionEuroSymbol extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/event-seat", JSImport.Default)
  object ActionEventSeat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/event", JSImport.Default)
  object ActionEvent extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/exit-to-app", JSImport.Default)
  object ActionExitToApp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/explore", JSImport.Default)
  object ActionExplore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/extension", JSImport.Default)
  object ActionExtension extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/face", JSImport.Default)
  object ActionFace extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/favorite-border", JSImport.Default)
  object ActionFavoriteBorder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/favorite", JSImport.Default)
  object ActionFavorite extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/feedback", JSImport.Default)
  object ActionFeedback extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/find-in-page", JSImport.Default)
  object ActionFindInPage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/find-replace", JSImport.Default)
  object ActionFindReplace extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/fingerprint", JSImport.Default)
  object ActionFingerprint extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/flight-land", JSImport.Default)
  object ActionFlightLand extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/flight-takeoff", JSImport.Default)
  object ActionFlightTakeoff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/flip-to-back", JSImport.Default)
  object ActionFlipToBack extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/flip-to-front", JSImport.Default)
  object ActionFlipToFront extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/g-translate", JSImport.Default)
  object ActionGTranslate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/gavel", JSImport.Default)
  object ActionGavel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/get-app", JSImport.Default)
  object ActionGetApp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/gif", JSImport.Default)
  object ActionGif extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/grade", JSImport.Default)
  object ActionGrade extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/group-work", JSImport.Default)
  object ActionGroupWork extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/help-outline", JSImport.Default)
  object ActionHelpOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/help", JSImport.Default)
  object ActionHelp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/HighlightOff", JSImport.Default)
  object HighlightOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/HighlightOffOutlined", JSImport.Default)
  object HighlightOffOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/history", JSImport.Default)
  object ActionHistory extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/home", JSImport.Default)
  object ActionHome extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/hourglass-empty", JSImport.Default)
  object ActionHourglassEmpty extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/hourglass-full", JSImport.Default)
  object ActionHourglassFull extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/http", JSImport.Default)
  object ActionHttp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/https", JSImport.Default)
  object ActionHttps extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/important-devices", JSImport.Default)
  object ActionImportantDevices extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/info-outline", JSImport.Default)
  object ActionInfoOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/info", JSImport.Default)
  object ActionInfo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/input", JSImport.Default)
  object ActionInput extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/invert-colors", JSImport.Default)
  object ActionInvertColors extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/label-outline", JSImport.Default)
  object ActionLabelOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/label", JSImport.Default)
  object ActionLabel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/language", JSImport.Default)
  object ActionLanguage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/launch", JSImport.Default)
  object ActionLaunch extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/lightbulb-outline", JSImport.Default)
  object ActionLightbulbOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/line-style", JSImport.Default)
  object ActionLineStyle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/line-weight", JSImport.Default)
  object ActionLineWeight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/list", JSImport.Default)
  object ActionList extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/lock-open", JSImport.Default)
  object ActionLockOpen extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/lock-outline", JSImport.Default)
  object ActionLockOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/lock", JSImport.Default)
  object ActionLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/loyalty", JSImport.Default)
  object ActionLoyalty extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/markunread-mailbox", JSImport.Default)
  object ActionMarkunreadMailbox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/motorcycle", JSImport.Default)
  object ActionMotorcycle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/note-add", JSImport.Default)
  object ActionNoteAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/offline-pin", JSImport.Default)
  object ActionOfflinePin extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/opacity", JSImport.Default)
  object ActionOpacity extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/open-in-browser", JSImport.Default)
  object ActionOpenInBrowser extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/open-in-new", JSImport.Default)
  object ActionOpenInNew extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/open-with", JSImport.Default)
  object ActionOpenWith extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/pageview", JSImport.Default)
  object ActionPageview extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/pan-tool", JSImport.Default)
  object ActionPanTool extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Payment", JSImport.Default)
  object Payment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-camera-mic", JSImport.Default)
  object ActionPermCameraMic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-contact-calendar", JSImport.Default)
  object ActionPermContactCalendar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-data-setting", JSImport.Default)
  object ActionPermDataSetting extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-device-information", JSImport.Default)
  object ActionPermDeviceInformation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-identity", JSImport.Default)
  object ActionPermIdentity extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-media", JSImport.Default)
  object ActionPermMedia extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-phone-msg", JSImport.Default)
  object ActionPermPhoneMsg extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/perm-scan-wifi", JSImport.Default)
  object ActionPermScanWifi extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/pets", JSImport.Default)
  object ActionPets extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/picture-in-picture-alt", JSImport.Default)
  object ActionPictureInPictureAlt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/picture-in-picture", JSImport.Default)
  object ActionPictureInPicture extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/play-for-work", JSImport.Default)
  object ActionPlayForWork extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/polymer", JSImport.Default)
  object ActionPolymer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/power-settings-new", JSImport.Default)
  object ActionPowerSettingsNew extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/pregnant-woman", JSImport.Default)
  object ActionPregnantWoman extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/print", JSImport.Default)
  object ActionPrint extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/query-builder", JSImport.Default)
  object ActionQueryBuilder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/question-answer", JSImport.Default)
  object ActionQuestionAnswer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Receipt", JSImport.Default)
  object Receipt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/record-voice-over", JSImport.Default)
  object ActionRecordVoiceOver extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/redeem", JSImport.Default)
  object ActionRedeem extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/remove-shopping-cart", JSImport.Default)
  object ActionRemoveShoppingCart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/reorder", JSImport.Default)
  object ActionReorder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/report-problem", JSImport.Default)
  object ActionReportProblem extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/restore-page", JSImport.Default)
  object ActionRestorePage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/restore", JSImport.Default)
  object ActionRestore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/room", JSImport.Default)
  object ActionRoom extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/rounded-corner", JSImport.Default)
  object ActionRoundedCorner extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/rowing", JSImport.Default)
  object ActionRowing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/schedule", JSImport.Default)
  object ActionSchedule extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/search", JSImport.Default)
  object ActionSearch extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-applications", JSImport.Default)
  object ActionSettingsApplications extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-backup-restore", JSImport.Default)
  object ActionSettingsBackupRestore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-bluetooth", JSImport.Default)
  object ActionSettingsBluetooth extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-brightness", JSImport.Default)
  object ActionSettingsBrightness extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-cell", JSImport.Default)
  object ActionSettingsCell extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-ethernet", JSImport.Default)
  object ActionSettingsEthernet extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-input-antenna", JSImport.Default)
  object ActionSettingsInputAntenna extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-input-component", JSImport.Default)
  object ActionSettingsInputComponent extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-input-composite", JSImport.Default)
  object ActionSettingsInputComposite extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-input-hdmi", JSImport.Default)
  object ActionSettingsInputHdmi extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-input-svideo", JSImport.Default)
  object ActionSettingsInputSvideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-overscan", JSImport.Default)
  object ActionSettingsOverscan extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-phone", JSImport.Default)
  object ActionSettingsPhone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-power", JSImport.Default)
  object ActionSettingsPower extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-remote", JSImport.Default)
  object ActionSettingsRemote extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/settings-voice", JSImport.Default)
  object ActionSettingsVoice extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Settings", JSImport.Default)
  object Settings extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/shop-two", JSImport.Default)
  object ActionShopTwo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/shop", JSImport.Default)
  object ActionShop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/shopping-basket", JSImport.Default)
  object ActionShoppingBasket extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/shopping-cart", JSImport.Default)
  object ActionShoppingCart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/speaker-notes-off", JSImport.Default)
  object ActionSpeakerNotesOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/speaker-notes", JSImport.Default)
  object ActionSpeakerNotes extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/spellcheck", JSImport.Default)
  object ActionSpellcheck extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Stars", JSImport.Default)
  object Stars extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Store", JSImport.Default)
  object Store extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Subject", JSImport.Default)
  object Subject extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/supervisor-account", JSImport.Default)
  object ActionSupervisorAccount extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/swap-horiz", JSImport.Default)
  object ActionSwapHoriz extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/swap-vert", JSImport.Default)
  object ActionSwapVert extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/swap-vertical-circle", JSImport.Default)
  object ActionSwapVerticalCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/system-update-alt", JSImport.Default)
  object ActionSystemUpdateAlt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/tab-unselected", JSImport.Default)
  object ActionTabUnselected extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/tab", JSImport.Default)
  object ActionTab extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/theaters", JSImport.Default)
  object ActionTheaters extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/three-d-rotation", JSImport.Default)
  object ActionThreeDRotation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/thumb-down", JSImport.Default)
  object ActionThumbDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/thumb-up", JSImport.Default)
  object ActionThumbUp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/thumbs-up-down", JSImport.Default)
  object ActionThumbsUpDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/timeline", JSImport.Default)
  object ActionTimeline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/toc", JSImport.Default)
  object ActionToc extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/today", JSImport.Default)
  object ActionToday extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/toll", JSImport.Default)
  object ActionToll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/TouchApp", JSImport.Default)
  object TouchApp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/track-changes", JSImport.Default)
  object ActionTrackChanges extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/translate", JSImport.Default)
  object ActionTranslate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/TrendingDown", JSImport.Default)
  object TrendingDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/TrendingFlat", JSImport.Default)
  object TrendingFlat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/TrendingUp", JSImport.Default)
  object TrendingUp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/turned-in-not", JSImport.Default)
  object ActionTurnedInNot extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/turned-in", JSImport.Default)
  object ActionTurnedIn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/update", JSImport.Default)
  object ActionUpdate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/verified-user", JSImport.Default)
  object ActionVerifiedUser extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-agenda", JSImport.Default)
  object ActionViewAgenda extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-array", JSImport.Default)
  object ActionViewArray extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-carousel", JSImport.Default)
  object ActionViewCarousel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-column", JSImport.Default)
  object ActionViewColumn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-day", JSImport.Default)
  object ActionViewDay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-headline", JSImport.Default)
  object ActionViewHeadline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-list", JSImport.Default)
  object ActionViewList extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-module", JSImport.Default)
  object ActionViewModule extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-quilt", JSImport.Default)
  object ActionViewQuilt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-stream", JSImport.Default)
  object ActionViewStream extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/view-week", JSImport.Default)
  object ActionViewWeek extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/visibility-off", JSImport.Default)
  object ActionVisibilityOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/visibility", JSImport.Default)
  object ActionVisibility extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/watch-later", JSImport.Default)
  object ActionWatchLater extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/work", JSImport.Default)
  object ActionWork extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/youtube-searched-for", JSImport.Default)
  object ActionYoutubeSearchedFor extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/zoom-in", JSImport.Default)
  object ActionZoomIn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/action/zoom-out", JSImport.Default)
  object ActionZoomOut extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AddAlert", JSImport.Default)
  object AddAlert extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ErrorOutline", JSImport.Default)
  object ErrorOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Error", JSImport.Default)
  object Error extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Warning", JSImport.Default)
  object Warning extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/NotificationImportant", JSImport.Default)
  object NotificationImportant extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/add-to-queue", JSImport.Default)
  object AvAddToQueue extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/airplay", JSImport.Default)
  object AvAirplay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/album", JSImport.Default)
  object AvAlbum extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/art-track", JSImport.Default)
  object AvArtTrack extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AvTimer", JSImport.Default)
  object AvTimer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/branding-watermark", JSImport.Default)
  object AvBrandingWatermark extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/call-to-action", JSImport.Default)
  object AvCallToAction extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/closed-caption", JSImport.Default)
  object AvClosedCaption extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/equalizer", JSImport.Default)
  object AvEqualizer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/explicit", JSImport.Default)
  object AvExplicit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/FastForward", JSImport.Default)
  object FastForward extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/FastRewind", JSImport.Default)
  object FastRewind extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/featured-play-list", JSImport.Default)
  object AvFeaturedPlayList extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/featured-video", JSImport.Default)
  object AvFeaturedVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/fiber-dvr", JSImport.Default)
  object AvFiberDvr extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/fiber-manual-record", JSImport.Default)
  object AvFiberManualRecord extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/fiber-new", JSImport.Default)
  object AvFiberNew extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/fiber-pin", JSImport.Default)
  object AvFiberPin extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/fiber-smart-record", JSImport.Default)
  object AvFiberSmartRecord extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/forward-10", JSImport.Default)
  object AvForward10 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/forward-30", JSImport.Default)
  object AvForward30 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/forward-5", JSImport.Default)
  object AvForward5 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/games", JSImport.Default)
  object AvGames extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/hd", JSImport.Default)
  object AvHd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/hearing", JSImport.Default)
  object AvHearing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/high-quality", JSImport.Default)
  object AvHighQuality extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/library-add", JSImport.Default)
  object AvLibraryAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/library-books", JSImport.Default)
  object AvLibraryBooks extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/library-music", JSImport.Default)
  object AvLibraryMusic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/loop", JSImport.Default)
  object AvLoop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/mic-none", JSImport.Default)
  object AvMicNone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/mic-off", JSImport.Default)
  object AvMicOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/mic", JSImport.Default)
  object AvMic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/movie", JSImport.Default)
  object AvMovie extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/music-video", JSImport.Default)
  object AvMusicVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/new-releases", JSImport.Default)
  object AvNewReleases extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/not-interested", JSImport.Default)
  object AvNotInterested extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/note", JSImport.Default)
  object AvNote extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/pause-circle-filled", JSImport.Default)
  object AvPauseCircleFilled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/pause-circle-outline", JSImport.Default)
  object AvPauseCircleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/pause", JSImport.Default)
  object AvPause extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/PlayArrow", JSImport.Default)
  object PlayArrow extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/play-circle-filled", JSImport.Default)
  object AvPlayCircleFilled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/play-circle-outline", JSImport.Default)
  object AvPlayCircleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/playlist-add-check", JSImport.Default)
  object AvPlaylistAddCheck extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/playlist-add", JSImport.Default)
  object AvPlaylistAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/playlist-play", JSImport.Default)
  object AvPlaylistPlay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/queue-music", JSImport.Default)
  object AvQueueMusic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/queue-play-next", JSImport.Default)
  object AvQueuePlayNext extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/queue", JSImport.Default)
  object AvQueue extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/radio", JSImport.Default)
  object AvRadio extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/recent-actors", JSImport.Default)
  object AvRecentActors extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/remove-from-queue", JSImport.Default)
  object AvRemoveFromQueue extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/repeat-one", JSImport.Default)
  object AvRepeatOne extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/repeat", JSImport.Default)
  object AvRepeat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/replay-10", JSImport.Default)
  object AvReplay10 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/replay-30", JSImport.Default)
  object AvReplay30 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/replay-5", JSImport.Default)
  object AvReplay5 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/replay", JSImport.Default)
  object AvReplay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/shuffle", JSImport.Default)
  object AvShuffle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/SkipNext", JSImport.Default)
  object SkipNext extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/SkipPrevious", JSImport.Default)
  object SkipPrevious extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/slow-motion-video", JSImport.Default)
  object AvSlowMotionVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/snooze", JSImport.Default)
  object AvSnooze extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/sort-by-alpha", JSImport.Default)
  object AvSortByAlpha extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/stop", JSImport.Default)
  object AvStop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/subscriptions", JSImport.Default)
  object AvSubscriptions extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/subtitles", JSImport.Default)
  object AvSubtitles extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/surround-sound", JSImport.Default)
  object AvSurroundSound extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/video-call", JSImport.Default)
  object AvVideoCall extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/video-label", JSImport.Default)
  object AvVideoLabel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/video-library", JSImport.Default)
  object AvVideoLibrary extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/videocam-off", JSImport.Default)
  object AvVideocamOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/videocam", JSImport.Default)
  object AvVideocam extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/volume-down", JSImport.Default)
  object AvVolumeDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/volume-mute", JSImport.Default)
  object AvVolumeMute extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/volume-off", JSImport.Default)
  object AvVolumeOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/volume-up", JSImport.Default)
  object AvVolumeUp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/web-asset", JSImport.Default)
  object AvWebAsset extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/av/web", JSImport.Default)
  object AvWeb extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/business", JSImport.Default)
  object CommunicationBusiness extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-end", JSImport.Default)
  object CommunicationCallEnd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-made", JSImport.Default)
  object CommunicationCallMade extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-merge", JSImport.Default)
  object CommunicationCallMerge extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-missed-outgoing", JSImport.Default)
  object CommunicationCallMissedOutgoing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-missed", JSImport.Default)
  object CommunicationCallMissed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-received", JSImport.Default)
  object CommunicationCallReceived extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call-split", JSImport.Default)
  object CommunicationCallSplit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/call", JSImport.Default)
  object CommunicationCall extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/chat-bubble-outline", JSImport.Default)
  object CommunicationChatBubbleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/chat-bubble", JSImport.Default)
  object CommunicationChatBubble extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/chat", JSImport.Default)
  object CommunicationChat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/clear-all", JSImport.Default)
  object CommunicationClearAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/comment", JSImport.Default)
  object CommunicationComment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/contact-mail", JSImport.Default)
  object CommunicationContactMail extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/contact-phone", JSImport.Default)
  object CommunicationContactPhone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/contacts", JSImport.Default)
  object CommunicationContacts extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/dialer-sip", JSImport.Default)
  object CommunicationDialerSip extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/dialpad", JSImport.Default)
  object CommunicationDialpad extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/email", JSImport.Default)
  object CommunicationEmail extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/forum", JSImport.Default)
  object CommunicationForum extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/import-contacts", JSImport.Default)
  object CommunicationImportContacts extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/import-export", JSImport.Default)
  object CommunicationImportExport extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/invert-colors-off", JSImport.Default)
  object CommunicationInvertColorsOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/live-help", JSImport.Default)
  object CommunicationLiveHelp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocationOff", JSImport.Default)
  object LocationOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocationOn", JSImport.Default)
  object LocationOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/mail-outline", JSImport.Default)
  object CommunicationMailOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/message", JSImport.Default)
  object CommunicationMessage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/no-sim", JSImport.Default)
  object CommunicationNoSim extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/phone", JSImport.Default)
  object CommunicationPhone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/phonelink-erase", JSImport.Default)
  object CommunicationPhonelinkErase extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/phonelink-lock", JSImport.Default)
  object CommunicationPhonelinkLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/phonelink-ring", JSImport.Default)
  object CommunicationPhonelinkRing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/phonelink-setup", JSImport.Default)
  object CommunicationPhonelinkSetup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/portable-wifi-off", JSImport.Default)
  object CommunicationPortableWifiOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/present-to-all", JSImport.Default)
  object CommunicationPresentToAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/ring-volume", JSImport.Default)
  object CommunicationRingVolume extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/rss-feed", JSImport.Default)
  object CommunicationRssFeed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/screen-share", JSImport.Default)
  object CommunicationScreenShare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/speaker-phone", JSImport.Default)
  object CommunicationSpeakerPhone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/stay-current-landscape",
                       JSImport.Default)
  object CommunicationStayCurrentLandscape extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/stay-current-portrait",
                       JSImport.Default)
  object CommunicationStayCurrentPortrait extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/stay-primary-landscape",
                       JSImport.Default)
  object CommunicationStayPrimaryLandscape extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/stay-primary-portrait",
                       JSImport.Default)
  object CommunicationStayPrimaryPortrait extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/stop-screen-share", JSImport.Default)
  object CommunicationStopScreenShare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/swap-calls", JSImport.Default)
  object CommunicationSwapCalls extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/textsms", JSImport.Default)
  object CommunicationTextsms extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/voicemail", JSImport.Default)
  object CommunicationVoicemail extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/communication/vpn-key", JSImport.Default)
  object CommunicationVpnKey extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/add-box", JSImport.Default)
  object ContentAddBox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/add-circle-outline", JSImport.Default)
  object ContentAddCircleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/add-circle", JSImport.Default)
  object ContentAddCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/add", JSImport.Default)
  object ContentAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Archive", JSImport.Default)
  object Archive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Backspace", JSImport.Default)
  object Backspace extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BackspaceOutlined", JSImport.Default)
  object BackspaceOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Block", JSImport.Default)
  object Block extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Clear", JSImport.Default)
  object Clear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/content-copy", JSImport.Default)
  object ContentContentCopy extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/content-cut", JSImport.Default)
  object ContentContentCut extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/content-paste", JSImport.Default)
  object ContentContentPaste extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/create", JSImport.Default)
  object ContentCreate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/delete-sweep", JSImport.Default)
  object ContentDeleteSweep extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/drafts", JSImport.Default)
  object ContentDrafts extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/filter-list", JSImport.Default)
  object ContentFilterList extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/flag", JSImport.Default)
  object ContentFlag extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/font-download", JSImport.Default)
  object ContentFontDownload extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/forward", JSImport.Default)
  object ContentForward extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/gesture", JSImport.Default)
  object ContentGesture extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Inbox", JSImport.Default)
  object Inbox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Link", JSImport.Default)
  object Link extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LinkOff", JSImport.Default)
  object LinkOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LowPriority", JSImport.Default)
  object LowPriority extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/mail", JSImport.Default)
  object ContentMail extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/markunread", JSImport.Default)
  object ContentMarkunread extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/move-to-inbox", JSImport.Default)
  object ContentMoveToInbox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/next-week", JSImport.Default)
  object ContentNextWeek extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Redo", JSImport.Default)
  object Redo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/remove-circle-outline", JSImport.Default)
  object ContentRemoveCircleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/remove-circle", JSImport.Default)
  object ContentRemoveCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/remove", JSImport.Default)
  object ContentRemove extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/reply-all", JSImport.Default)
  object ContentReplyAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/reply", JSImport.Default)
  object ContentReply extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Report", JSImport.Default)
  object Report extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/save", JSImport.Default)
  object ContentSave extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/select-all", JSImport.Default)
  object ContentSelectAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/send", JSImport.Default)
  object ContentSend extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/sort", JSImport.Default)
  object ContentSort extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/text-format", JSImport.Default)
  object ContentTextFormat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/unarchive", JSImport.Default)
  object ContentUnarchive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Undo", JSImport.Default)
  object Undo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/content/weekend", JSImport.Default)
  object ContentWeekend extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AccessAlarm", JSImport.Default)
  object AccessAlarm extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AccessAlarms", JSImport.Default)
  object AccessAlarms extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AccessTime", JSImport.Default)
  object AccessTime extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/add-alarm", JSImport.Default)
  object DeviceAddAlarm extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/airplanemode-active", JSImport.Default)
  object DeviceAirplanemodeActive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/airplanemode-inactive", JSImport.Default)
  object DeviceAirplanemodeInactive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-20", JSImport.Default)
  object DeviceBattery20 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-30", JSImport.Default)
  object DeviceBattery30 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-50", JSImport.Default)
  object DeviceBattery50 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-60", JSImport.Default)
  object DeviceBattery60 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-80", JSImport.Default)
  object DeviceBattery80 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-90", JSImport.Default)
  object DeviceBattery90 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-alert", JSImport.Default)
  object DeviceBatteryAlert extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-20", JSImport.Default)
  object DeviceBatteryCharging20 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-30", JSImport.Default)
  object DeviceBatteryCharging30 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-50", JSImport.Default)
  object DeviceBatteryCharging50 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-60", JSImport.Default)
  object DeviceBatteryCharging60 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-80", JSImport.Default)
  object DeviceBatteryCharging80 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-90", JSImport.Default)
  object DeviceBatteryCharging90 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-charging-full", JSImport.Default)
  object DeviceBatteryChargingFull extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-full", JSImport.Default)
  object DeviceBatteryFull extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-std", JSImport.Default)
  object DeviceBatteryStd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/battery-unknown", JSImport.Default)
  object DeviceBatteryUnknown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BluetoothConnected", JSImport.Default)
  object BluetoothConnected extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BluetoothDisabled", JSImport.Default)
  object BluetoothDisabled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BluetoothSearching", JSImport.Default)
  object BluetoothSearching extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Bluetooth", JSImport.Default)
  object Bluetooth extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/brightness-auto", JSImport.Default)
  object DeviceBrightnessAuto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/brightness-high", JSImport.Default)
  object DeviceBrightnessHigh extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/brightness-low", JSImport.Default)
  object DeviceBrightnessLow extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/brightness-medium", JSImport.Default)
  object DeviceBrightnessMedium extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/data-usage", JSImport.Default)
  object DeviceDataUsage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/developer-mode", JSImport.Default)
  object DeviceDeveloperMode extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/devices", JSImport.Default)
  object DeviceDevices extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/dvr", JSImport.Default)
  object DeviceDvr extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/GpsFixed", JSImport.Default)
  object GpsFixed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/GpsNotFixed", JSImport.Default)
  object GpsNotFixed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/GpsOff", JSImport.Default)
  object GpsOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/GraphicEq", JSImport.Default)
  object GraphicEq extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocationDisabled", JSImport.Default)
  object LocationDisabled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocationSearching", JSImport.Default)
  object LocationSearching extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/network-cell", JSImport.Default)
  object DeviceNetworkCell extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/network-wifi", JSImport.Default)
  object DeviceNetworkWifi extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/nfc", JSImport.Default)
  object DeviceNfc extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/screen-lock-landscape", JSImport.Default)
  object DeviceScreenLockLandscape extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/screen-lock-portrait", JSImport.Default)
  object DeviceScreenLockPortrait extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/screen-lock-rotation", JSImport.Default)
  object DeviceScreenLockRotation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/screen-rotation", JSImport.Default)
  object DeviceScreenRotation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/sd-storage", JSImport.Default)
  object DeviceSdStorage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/settings-system-daydream", JSImport.Default)
  object DeviceSettingsSystemDaydream extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-0-bar", JSImport.Default)
  object DeviceSignalCellular0Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-1-bar", JSImport.Default)
  object DeviceSignalCellular1Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-2-bar", JSImport.Default)
  object DeviceSignalCellular2Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-3-bar", JSImport.Default)
  object DeviceSignalCellular3Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-4-bar", JSImport.Default)
  object DeviceSignalCellular4Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-connected-no-internet-0-bar",
                       JSImport.Default)
  object DeviceSignalCellularConnectedNoInternet0Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-connected-no-internet-1-bar",
                       JSImport.Default)
  object DeviceSignalCellularConnectedNoInternet1Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-connected-no-internet-2-bar",
                       JSImport.Default)
  object DeviceSignalCellularConnectedNoInternet2Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-connected-no-internet-3-bar",
                       JSImport.Default)
  object DeviceSignalCellularConnectedNoInternet3Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-connected-no-internet-4-bar",
                       JSImport.Default)
  object DeviceSignalCellularConnectedNoInternet4Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-no-sim", JSImport.Default)
  object DeviceSignalCellularNoSim extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-null", JSImport.Default)
  object DeviceSignalCellularNull extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-cellular-off", JSImport.Default)
  object DeviceSignalCellularOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-0-bar", JSImport.Default)
  object DeviceSignalWifi0Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-1-bar-lock", JSImport.Default)
  object DeviceSignalWifi1BarLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-1-bar", JSImport.Default)
  object DeviceSignalWifi1Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-2-bar-lock", JSImport.Default)
  object DeviceSignalWifi2BarLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-2-bar", JSImport.Default)
  object DeviceSignalWifi2Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-3-bar-lock", JSImport.Default)
  object DeviceSignalWifi3BarLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-3-bar", JSImport.Default)
  object DeviceSignalWifi3Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-4-bar-lock", JSImport.Default)
  object DeviceSignalWifi4BarLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-4-bar", JSImport.Default)
  object DeviceSignalWifi4Bar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/signal-wifi-off", JSImport.Default)
  object DeviceSignalWifiOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/storage", JSImport.Default)
  object DeviceStorage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/usb", JSImport.Default)
  object DeviceUsb extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/wallpaper", JSImport.Default)
  object DeviceWallpaper extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/widgets", JSImport.Default)
  object DeviceWidgets extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/wifi-lock", JSImport.Default)
  object DeviceWifiLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/device/wifi-tethering", JSImport.Default)
  object DeviceWifiTethering extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/attach-file", JSImport.Default)
  object EditorAttachFile extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/attach-money", JSImport.Default)
  object EditorAttachMoney extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderAll", JSImport.Default)
  object BorderAll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderBottom", JSImport.Default)
  object BorderBottom extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderClear", JSImport.Default)
  object BorderClear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderColor", JSImport.Default)
  object BorderColor extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderHorizontal", JSImport.Default)
  object BorderHorizontal extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderInner", JSImport.Default)
  object BorderInner extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderLeft", JSImport.Default)
  object BorderLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderOuter", JSImport.Default)
  object BorderOuter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderRight", JSImport.Default)
  object BorderRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderStyle", JSImport.Default)
  object BorderStyle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderTop", JSImport.Default)
  object BorderTop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BorderVertical", JSImport.Default)
  object BorderVertical extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/BubbleChart", JSImport.Default)
  object BubbleChart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/drag-handle", JSImport.Default)
  object EditorDragHandle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-align-center", JSImport.Default)
  object EditorFormatAlignCenter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-align-justify", JSImport.Default)
  object EditorFormatAlignJustify extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-align-left", JSImport.Default)
  object EditorFormatAlignLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-align-right", JSImport.Default)
  object EditorFormatAlignRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-bold", JSImport.Default)
  object EditorFormatBold extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-clear", JSImport.Default)
  object EditorFormatClear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-color-fill", JSImport.Default)
  object EditorFormatColorFill extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-color-reset", JSImport.Default)
  object EditorFormatColorReset extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-color-text", JSImport.Default)
  object EditorFormatColorText extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-indent-decrease", JSImport.Default)
  object EditorFormatIndentDecrease extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-indent-increase", JSImport.Default)
  object EditorFormatIndentIncrease extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-italic", JSImport.Default)
  object EditorFormatItalic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-line-spacing", JSImport.Default)
  object EditorFormatLineSpacing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-list-bulleted", JSImport.Default)
  object EditorFormatListBulleted extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-list-numbered", JSImport.Default)
  object EditorFormatListNumbered extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-paint", JSImport.Default)
  object EditorFormatPaint extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-quote", JSImport.Default)
  object EditorFormatQuote extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-shapes", JSImport.Default)
  object EditorFormatShapes extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-size", JSImport.Default)
  object EditorFormatSize extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-strikethrough", JSImport.Default)
  object EditorFormatStrikethrough extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-textdirection-l-to-r", JSImport.Default)
  object EditorFormatTextdirectionLToR extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-textdirection-r-to-l", JSImport.Default)
  object EditorFormatTextdirectionRToL extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/format-underlined", JSImport.Default)
  object EditorFormatUnderlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/functions", JSImport.Default)
  object EditorFunctions extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Highlight", JSImport.Default)
  object Highlight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-chart", JSImport.Default)
  object EditorInsertChart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-comment", JSImport.Default)
  object EditorInsertComment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-drive-file", JSImport.Default)
  object EditorInsertDriveFile extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-emoticon", JSImport.Default)
  object EditorInsertEmoticon extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-invitation", JSImport.Default)
  object EditorInsertInvitation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-link", JSImport.Default)
  object EditorInsertLink extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/insert-photo", JSImport.Default)
  object EditorInsertPhoto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/linear-scale", JSImport.Default)
  object EditorLinearScale extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/merge-type", JSImport.Default)
  object EditorMergeType extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/mode-comment", JSImport.Default)
  object EditorModeComment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/mode-edit", JSImport.Default)
  object EditorModeEdit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/monetization-on", JSImport.Default)
  object EditorMonetizationOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/money-off", JSImport.Default)
  object EditorMoneyOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/multiline-chart", JSImport.Default)
  object EditorMultilineChart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/pie-chart-outlined", JSImport.Default)
  object EditorPieChartOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/pie-chart", JSImport.Default)
  object EditorPieChart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/publish", JSImport.Default)
  object EditorPublish extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ShortText", JSImport.Default)
  object ShortText extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/show-chart", JSImport.Default)
  object EditorShowChart extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/space-bar", JSImport.Default)
  object EditorSpaceBar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/strikethrough-s", JSImport.Default)
  object EditorStrikethroughS extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/text-fields", JSImport.Default)
  object EditorTextFields extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/title", JSImport.Default)
  object EditorTitle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/vertical-align-bottom", JSImport.Default)
  object EditorVerticalAlignBottom extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/vertical-align-center", JSImport.Default)
  object EditorVerticalAlignCenter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/vertical-align-top", JSImport.Default)
  object EditorVerticalAlignTop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/editor/wrap-text", JSImport.Default)
  object EditorWrapText extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/attachment", JSImport.Default)
  object FileAttachment extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-circle", JSImport.Default)
  object FileCloudCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-done", JSImport.Default)
  object FileCloudDone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-download", JSImport.Default)
  object FileCloudDownload extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-off", JSImport.Default)
  object FileCloudOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-queue", JSImport.Default)
  object FileCloudQueue extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud-upload", JSImport.Default)
  object FileCloudUpload extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/cloud", JSImport.Default)
  object FileCloud extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/create-new-folder", JSImport.Default)
  object FileCreateNewFolder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/file-download", JSImport.Default)
  object FileFileDownload extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/file-upload", JSImport.Default)
  object FileFileUpload extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/folder-open", JSImport.Default)
  object FileFolderOpen extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/folder-shared", JSImport.Default)
  object FileFolderShared extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/file/folder", JSImport.Default)
  object FileFolder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/cast-connected", JSImport.Default)
  object HardwareCastConnected extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/cast", JSImport.Default)
  object HardwareCast extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/computer", JSImport.Default)
  object HardwareComputer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/desktop-mac", JSImport.Default)
  object HardwareDesktopMac extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/desktop-windows", JSImport.Default)
  object HardwareDesktopWindows extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/developer-board", JSImport.Default)
  object HardwareDeveloperBoard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/device-hub", JSImport.Default)
  object HardwareDeviceHub extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/devices-other", JSImport.Default)
  object HardwareDevicesOther extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/dock", JSImport.Default)
  object HardwareDock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/gamepad", JSImport.Default)
  object HardwareGamepad extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/headset-mic", JSImport.Default)
  object HardwareHeadsetMic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/headset", JSImport.Default)
  object HardwareHeadset extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/KeyboardArrowDown", JSImport.Default)
  object KeyboardArrowDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/KeyboardArrowLeft", JSImport.Default)
  object KeyboardArrowLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/KeyboardArrowRight", JSImport.Default)
  object KeyboardArrowRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/KeyboardArrowUp", JSImport.Default)
  object KeyboardArrowUp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-backspace", JSImport.Default)
  object HardwareKeyboardBackspace extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-capslock", JSImport.Default)
  object HardwareKeyboardCapslock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-hide", JSImport.Default)
  object HardwareKeyboardHide extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-return", JSImport.Default)
  object HardwareKeyboardReturn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-tab", JSImport.Default)
  object HardwareKeyboardTab extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard-voice", JSImport.Default)
  object HardwareKeyboardVoice extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/keyboard", JSImport.Default)
  object HardwareKeyboard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/laptop-chromebook", JSImport.Default)
  object HardwareLaptopChromebook extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/laptop-mac", JSImport.Default)
  object HardwareLaptopMac extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/laptop-windows", JSImport.Default)
  object HardwareLaptopWindows extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/laptop", JSImport.Default)
  object HardwareLaptop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/memory", JSImport.Default)
  object HardwareMemory extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Mouse", JSImport.Default)
  object Mouse extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/phone-android", JSImport.Default)
  object HardwarePhoneAndroid extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/phone-iphone", JSImport.Default)
  object HardwarePhoneIphone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/phonelink-off", JSImport.Default)
  object HardwarePhonelinkOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/phonelink", JSImport.Default)
  object HardwarePhonelink extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/power-input", JSImport.Default)
  object HardwarePowerInput extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/router", JSImport.Default)
  object HardwareRouter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/scanner", JSImport.Default)
  object HardwareScanner extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/security", JSImport.Default)
  object HardwareSecurity extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/sim-card", JSImport.Default)
  object HardwareSimCard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/smartphone", JSImport.Default)
  object HardwareSmartphone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/speaker-group", JSImport.Default)
  object HardwareSpeakerGroup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/speaker", JSImport.Default)
  object HardwareSpeaker extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/tablet-android", JSImport.Default)
  object HardwareTabletAndroid extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/tablet-mac", JSImport.Default)
  object HardwareTabletMac extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/tablet", JSImport.Default)
  object HardwareTablet extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/toys", JSImport.Default)
  object HardwareToys extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/tv", JSImport.Default)
  object HardwareTv extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/videogame-asset", JSImport.Default)
  object HardwareVideogameAsset extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/hardware/watch", JSImport.Default)
  object HardwareWatch extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/add-a-photo", JSImport.Default)
  object ImageAddAPhoto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/add-to-photos", JSImport.Default)
  object ImageAddToPhotos extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/adjust", JSImport.Default)
  object ImageAdjust extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/assistant-photo", JSImport.Default)
  object ImageAssistantPhoto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/assistant", JSImport.Default)
  object ImageAssistant extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/audiotrack", JSImport.Default)
  object ImageAudiotrack extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/blur-circular", JSImport.Default)
  object ImageBlurCircular extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/blur-linear", JSImport.Default)
  object ImageBlurLinear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/blur-off", JSImport.Default)
  object ImageBlurOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/blur-on", JSImport.Default)
  object ImageBlurOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-1", JSImport.Default)
  object ImageBrightness1 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-2", JSImport.Default)
  object ImageBrightness2 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-3", JSImport.Default)
  object ImageBrightness3 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-4", JSImport.Default)
  object ImageBrightness4 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-5", JSImport.Default)
  object ImageBrightness5 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-6", JSImport.Default)
  object ImageBrightness6 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brightness-7", JSImport.Default)
  object ImageBrightness7 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/broken-image", JSImport.Default)
  object ImageBrokenImage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/brush", JSImport.Default)
  object ImageBrush extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/burst-mode", JSImport.Default)
  object ImageBurstMode extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/camera-alt", JSImport.Default)
  object ImageCameraAlt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/camera-front", JSImport.Default)
  object ImageCameraFront extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/camera-rear", JSImport.Default)
  object ImageCameraRear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/camera-roll", JSImport.Default)
  object ImageCameraRoll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/camera", JSImport.Default)
  object ImageCamera extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/center-focus-strong", JSImport.Default)
  object ImageCenterFocusStrong extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/center-focus-weak", JSImport.Default)
  object ImageCenterFocusWeak extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/collections-bookmark", JSImport.Default)
  object ImageCollectionsBookmark extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/collections", JSImport.Default)
  object ImageCollections extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/color-lens", JSImport.Default)
  object ImageColorLens extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/colorize", JSImport.Default)
  object ImageColorize extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/compare", JSImport.Default)
  object ImageCompare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/control-point-duplicate", JSImport.Default)
  object ImageControlPointDuplicate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/control-point", JSImport.Default)
  object ImageControlPoint extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-16-9", JSImport.Default)
  object ImageCrop169 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-3-2", JSImport.Default)
  object ImageCrop32 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-5-4", JSImport.Default)
  object ImageCrop54 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-7-5", JSImport.Default)
  object ImageCrop75 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-din", JSImport.Default)
  object ImageCropDin extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-free", JSImport.Default)
  object ImageCropFree extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-landscape", JSImport.Default)
  object ImageCropLandscape extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-original", JSImport.Default)
  object ImageCropOriginal extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-portrait", JSImport.Default)
  object ImageCropPortrait extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-rotate", JSImport.Default)
  object ImageCropRotate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop-square", JSImport.Default)
  object ImageCropSquare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/crop", JSImport.Default)
  object ImageCrop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/dehaze", JSImport.Default)
  object ImageDehaze extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/details", JSImport.Default)
  object ImageDetails extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Edit", JSImport.Default)
  object Edit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure-neg-1", JSImport.Default)
  object ImageExposureNeg1 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure-neg-2", JSImport.Default)
  object ImageExposureNeg2 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure-plus-1", JSImport.Default)
  object ImageExposurePlus1 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure-plus-2", JSImport.Default)
  object ImageExposurePlus2 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure-zero", JSImport.Default)
  object ImageExposureZero extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/exposure", JSImport.Default)
  object ImageExposure extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-1", JSImport.Default)
  object ImageFilter1 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-2", JSImport.Default)
  object ImageFilter2 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-3", JSImport.Default)
  object ImageFilter3 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-4", JSImport.Default)
  object ImageFilter4 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-5", JSImport.Default)
  object ImageFilter5 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-6", JSImport.Default)
  object ImageFilter6 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-7", JSImport.Default)
  object ImageFilter7 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-8", JSImport.Default)
  object ImageFilter8 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-9-plus", JSImport.Default)
  object ImageFilter9Plus extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-9", JSImport.Default)
  object ImageFilter9 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-b-and-w", JSImport.Default)
  object ImageFilterBAndW extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-center-focus", JSImport.Default)
  object ImageFilterCenterFocus extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-drama", JSImport.Default)
  object ImageFilterDrama extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-frames", JSImport.Default)
  object ImageFilterFrames extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-hdr", JSImport.Default)
  object ImageFilterHdr extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-none", JSImport.Default)
  object ImageFilterNone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-tilt-shift", JSImport.Default)
  object ImageFilterTiltShift extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter-vintage", JSImport.Default)
  object ImageFilterVintage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/filter", JSImport.Default)
  object ImageFilter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/flare", JSImport.Default)
  object ImageFlare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/flash-auto", JSImport.Default)
  object ImageFlashAuto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/flash-off", JSImport.Default)
  object ImageFlashOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/flash-on", JSImport.Default)
  object ImageFlashOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/flip", JSImport.Default)
  object ImageFlip extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/gradient", JSImport.Default)
  object ImageGradient extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/grain", JSImport.Default)
  object ImageGrain extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/grid-off", JSImport.Default)
  object ImageGridOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/grid-on", JSImport.Default)
  object ImageGridOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/hdr-off", JSImport.Default)
  object ImageHdrOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/hdr-on", JSImport.Default)
  object ImageHdrOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/hdr-strong", JSImport.Default)
  object ImageHdrStrong extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/hdr-weak", JSImport.Default)
  object ImageHdrWeak extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Healing", JSImport.Default)
  object Healing extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/image-aspect-ratio", JSImport.Default)
  object ImageImageAspectRatio extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/image", JSImport.Default)
  object ImageImage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/iso", JSImport.Default)
  object ImageIso extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/landscape", JSImport.Default)
  object ImageLandscape extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/leak-add", JSImport.Default)
  object ImageLeakAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/leak-remove", JSImport.Default)
  object ImageLeakRemove extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/lens", JSImport.Default)
  object ImageLens extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/linked-camera", JSImport.Default)
  object ImageLinkedCamera extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-3", JSImport.Default)
  object ImageLooks3 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-4", JSImport.Default)
  object ImageLooks4 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-5", JSImport.Default)
  object ImageLooks5 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-6", JSImport.Default)
  object ImageLooks6 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-one", JSImport.Default)
  object ImageLooksOne extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks-two", JSImport.Default)
  object ImageLooksTwo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/looks", JSImport.Default)
  object ImageLooks extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/loupe", JSImport.Default)
  object ImageLoupe extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/monochrome-photos", JSImport.Default)
  object ImageMonochromePhotos extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/movie-creation", JSImport.Default)
  object ImageMovieCreation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/movie-filter", JSImport.Default)
  object ImageMovieFilter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/music-note", JSImport.Default)
  object ImageMusicNote extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/nature-people", JSImport.Default)
  object ImageNaturePeople extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/nature", JSImport.Default)
  object ImageNature extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/navigate-before", JSImport.Default)
  object ImageNavigateBefore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/navigate-next", JSImport.Default)
  object ImageNavigateNext extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/palette", JSImport.Default)
  object ImagePalette extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/panorama-fish-eye", JSImport.Default)
  object ImagePanoramaFishEye extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/panorama-horizontal", JSImport.Default)
  object ImagePanoramaHorizontal extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/panorama-vertical", JSImport.Default)
  object ImagePanoramaVertical extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/panorama-wide-angle", JSImport.Default)
  object ImagePanoramaWideAngle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/panorama", JSImport.Default)
  object ImagePanorama extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-album", JSImport.Default)
  object ImagePhotoAlbum extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-camera", JSImport.Default)
  object ImagePhotoCamera extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-filter", JSImport.Default)
  object ImagePhotoFilter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-library", JSImport.Default)
  object ImagePhotoLibrary extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-size-select-actual", JSImport.Default)
  object ImagePhotoSizeSelectActual extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-size-select-large", JSImport.Default)
  object ImagePhotoSizeSelectLarge extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo-size-select-small", JSImport.Default)
  object ImagePhotoSizeSelectSmall extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/photo", JSImport.Default)
  object ImagePhoto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/picture-as-pdf", JSImport.Default)
  object ImagePictureAsPdf extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/portrait", JSImport.Default)
  object ImagePortrait extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/remove-red-eye", JSImport.Default)
  object ImageRemoveRedEye extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/rotate-90-degrees-ccw", JSImport.Default)
  object ImageRotate90DegreesCcw extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/rotate-left", JSImport.Default)
  object ImageRotateLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/rotate-right", JSImport.Default)
  object ImageRotateRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/slideshow", JSImport.Default)
  object ImageSlideshow extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/straighten", JSImport.Default)
  object ImageStraighten extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/style", JSImport.Default)
  object ImageStyle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/switch-camera", JSImport.Default)
  object ImageSwitchCamera extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/switch-video", JSImport.Default)
  object ImageSwitchVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/tag-faces", JSImport.Default)
  object ImageTagFaces extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/texture", JSImport.Default)
  object ImageTexture extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/timelapse", JSImport.Default)
  object ImageTimelapse extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/timer-10", JSImport.Default)
  object ImageTimer10 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/timer-3", JSImport.Default)
  object ImageTimer3 extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/timer-off", JSImport.Default)
  object ImageTimerOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/timer", JSImport.Default)
  object ImageTimer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/tonality", JSImport.Default)
  object ImageTonality extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/transform", JSImport.Default)
  object ImageTransform extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/tune", JSImport.Default)
  object ImageTune extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/view-comfy", JSImport.Default)
  object ImageViewComfy extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/view-compact", JSImport.Default)
  object ImageViewCompact extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/vignette", JSImport.Default)
  object ImageVignette extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/wb-auto", JSImport.Default)
  object ImageWbAuto extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/wb-cloudy", JSImport.Default)
  object ImageWbCloudy extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/wb-incandescent", JSImport.Default)
  object ImageWbIncandescent extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/image/wb-iridescent", JSImport.Default)
  object ImageWbIridescent extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/WbSunny", JSImport.Default)
  object WbSunny extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AddLocation", JSImport.Default)
  object AddLocation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/NotListedLocation", JSImport.Default)
  object NotListedLocation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Beenhere", JSImport.Default)
  object BeenHere extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-bike", JSImport.Default)
  object MapsDirectionsBike extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-boat", JSImport.Default)
  object MapsDirectionsBoat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-bus", JSImport.Default)
  object MapsDirectionsBus extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-car", JSImport.Default)
  object MapsDirectionsCar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-railway", JSImport.Default)
  object MapsDirectionsRailway extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-run", JSImport.Default)
  object MapsDirectionsRun extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-subway", JSImport.Default)
  object MapsDirectionsSubway extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-transit", JSImport.Default)
  object MapsDirectionsTransit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions-walk", JSImport.Default)
  object MapsDirectionsWalk extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/directions", JSImport.Default)
  object MapsDirections extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/EditLocation", JSImport.Default)
  object EditLocation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/ev-station", JSImport.Default)
  object MapsEvStation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/flight", JSImport.Default)
  object MapsFlight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/hotel", JSImport.Default)
  object MapsHotel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/layers-clear", JSImport.Default)
  object MapsLayersClear extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/layers", JSImport.Default)
  object MapsLayers extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-activity", JSImport.Default)
  object MapsLocalActivity extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-airport", JSImport.Default)
  object MapsLocalAirport extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-atm", JSImport.Default)
  object MapsLocalAtm extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-bar", JSImport.Default)
  object MapsLocalBar extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-cafe", JSImport.Default)
  object MapsLocalCafe extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-car-wash", JSImport.Default)
  object MapsLocalCarWash extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-convenience-store", JSImport.Default)
  object MapsLocalConvenienceStore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-dining", JSImport.Default)
  object MapsLocalDining extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-drink", JSImport.Default)
  object MapsLocalDrink extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-florist", JSImport.Default)
  object MapsLocalFlorist extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-gas-station", JSImport.Default)
  object MapsLocalGasStation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-grocery-store", JSImport.Default)
  object MapsLocalGroceryStore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-hospital", JSImport.Default)
  object MapsLocalHospital extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-hotel", JSImport.Default)
  object MapsLocalHotel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-laundry-service", JSImport.Default)
  object MapsLocalLaundryService extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-library", JSImport.Default)
  object MapsLocalLibrary extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-mall", JSImport.Default)
  object MapsLocalMall extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-movies", JSImport.Default)
  object MapsLocalMovies extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocalOffer", JSImport.Default)
  object LocalOffer extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-parking", JSImport.Default)
  object MapsLocalParking extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-pharmacy", JSImport.Default)
  object MapsLocalPharmacy extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-phone", JSImport.Default)
  object MapsLocalPhone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-pizza", JSImport.Default)
  object MapsLocalPizza extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-play", JSImport.Default)
  object MapsLocalPlay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-post-office", JSImport.Default)
  object MapsLocalPostOffice extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-printshop", JSImport.Default)
  object MapsLocalPrintshop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-see", JSImport.Default)
  object MapsLocalSee extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-shipping", JSImport.Default)
  object MapsLocalShipping extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/local-taxi", JSImport.Default)
  object MapsLocalTaxi extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/map", JSImport.Default)
  object MapsMap extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/MyLocation", JSImport.Default)
  object MyLocation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/navigation", JSImport.Default)
  object MapsNavigation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/near-me", JSImport.Default)
  object MapsNearMe extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/person-pin-circle", JSImport.Default)
  object MapsPersonPinCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/person-pin", JSImport.Default)
  object MapsPersonPin extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/pin-drop", JSImport.Default)
  object MapsPinDrop extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/place", JSImport.Default)
  object MapsPlace extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/rate-review", JSImport.Default)
  object MapsRateReview extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/restaurant-menu", JSImport.Default)
  object MapsRestaurantMenu extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/restaurant", JSImport.Default)
  object MapsRestaurant extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/satellite", JSImport.Default)
  object MapsSatellite extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/store-mall-directory", JSImport.Default)
  object MapsStoreMallDirectory extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/streetview", JSImport.Default)
  object MapsStreetview extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/subway", JSImport.Default)
  object MapsSubway extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/terrain", JSImport.Default)
  object MapsTerrain extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/traffic", JSImport.Default)
  object MapsTraffic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/train", JSImport.Default)
  object MapsTrain extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/tram", JSImport.Default)
  object MapsTram extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/transfer-within-a-station", JSImport.Default)
  object MapsTransferWithinAStation extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/maps/zoom-out-map", JSImport.Default)
  object MapsZoomOutMap extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/apps", JSImport.Default)
  object NavigationApps extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowBack", JSImport.Default)
  object ArrowBack extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowBackIos", JSImport.Default)
  object ArrowBackIos extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowBackIosOutlined", JSImport.Default)
  object ArrowBackIosOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowDownward", JSImport.Default)
  object ArrowDownward extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowDropDownCircle", JSImport.Default)
  object ArrowDropDownCircle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/ArrowDropDown", JSImport.Default)
  object ArrowDropDown extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowDropUp", JSImport.Default)
  object ArrowDropUp extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowForward", JSImport.Default)
  object ArrowForward extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowForwardIos", JSImport.Default)
  object ArrowForwardIos extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowForwardIosOutlined", JSImport.Default)
  object ArrowForwardIosOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ArrowUpward", JSImport.Default)
  object ArrowUpward extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Cancel", JSImport.Default)
  object Cancel extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/CancelOutlined", JSImport.Default)
  object CancelOutlined extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Check", JSImport.Default)
  object Check extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ChevronLeft", JSImport.Default)
  object ChevronLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ChevronRight", JSImport.Default)
  object ChevronRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Close", JSImport.Default)
  object Close extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ExpandLess", JSImport.Default)
  object ExpandLess extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/ExpandMore", JSImport.Default)
  object ExpandMore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/FirstPage", JSImport.Default)
  object FirstPage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/FullscreenExit", JSImport.Default)
  object FullscreenExit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Fullscreen", JSImport.Default)
  object Fullscreen extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LastPage", JSImport.Default)
  object LastPage extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Menu", JSImport.Default)
  object Menu extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/more-horiz", JSImport.Default)
  object NavigationMoreHoriz extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/more-vert", JSImport.Default)
  object NavigationMoreVert extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Refresh", JSImport.Default)
  object Refresh extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/subdirectory-arrow-left", JSImport.Default)
  object NavigationSubdirectoryArrowLeft extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/subdirectory-arrow-right",
                       JSImport.Default)
  object NavigationSubdirectoryArrowRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/unfold-less", JSImport.Default)
  object NavigationUnfoldLess extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation/unfold-more", JSImport.Default)
  object NavigationUnfoldMore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/navigation-arrow-drop-right", JSImport.Default)
  object NavigationArrowDropRight extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/adb", JSImport.Default)
  object NotificationAdb extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-flat-angled",
                       JSImport.Default)
  object NotificationAirlineSeatFlatAngled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-flat", JSImport.Default)
  object NotificationAirlineSeatFlat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-individual-suite",
                       JSImport.Default)
  object NotificationAirlineSeatIndividualSuite extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-legroom-extra",
                       JSImport.Default)
  object NotificationAirlineSeatLegroomExtra extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-legroom-normal",
                       JSImport.Default)
  object NotificationAirlineSeatLegroomNormal extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-legroom-reduced",
                       JSImport.Default)
  object NotificationAirlineSeatLegroomReduced extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-recline-extra",
                       JSImport.Default)
  object NotificationAirlineSeatReclineExtra extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/airline-seat-recline-normal",
                       JSImport.Default)
  object NotificationAirlineSeatReclineNormal extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/bluetooth-audio", JSImport.Default)
  object NotificationBluetoothAudio extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/confirmation-number", JSImport.Default)
  object NotificationConfirmationNumber extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/disc-full", JSImport.Default)
  object NotificationDiscFull extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/do-not-disturb-alt", JSImport.Default)
  object NotificationDoNotDisturbAlt extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/do-not-disturb-off", JSImport.Default)
  object NotificationDoNotDisturbOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/do-not-disturb-on", JSImport.Default)
  object NotificationDoNotDisturbOn extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/do-not-disturb", JSImport.Default)
  object NotificationDoNotDisturb extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/drive-eta", JSImport.Default)
  object NotificationDriveEta extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/enhanced-encryption", JSImport.Default)
  object NotificationEnhancedEncryption extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/event-available", JSImport.Default)
  object NotificationEventAvailable extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/event-busy", JSImport.Default)
  object NotificationEventBusy extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/event-note", JSImport.Default)
  object NotificationEventNote extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/folder-special", JSImport.Default)
  object NotificationFolderSpecial extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/live-tv", JSImport.Default)
  object NotificationLiveTv extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/mms", JSImport.Default)
  object NotificationMms extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/more", JSImport.Default)
  object NotificationMore extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/network-check", JSImport.Default)
  object NotificationNetworkCheck extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/network-locked", JSImport.Default)
  object NotificationNetworkLocked extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/no-encryption", JSImport.Default)
  object NotificationNoEncryption extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/ondemand-video", JSImport.Default)
  object NotificationOndemandVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/personal-video", JSImport.Default)
  object NotificationPersonalVideo extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-bluetooth-speaker",
                       JSImport.Default)
  object NotificationPhoneBluetoothSpeaker extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-forwarded", JSImport.Default)
  object NotificationPhoneForwarded extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-in-talk", JSImport.Default)
  object NotificationPhoneInTalk extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-locked", JSImport.Default)
  object NotificationPhoneLocked extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-missed", JSImport.Default)
  object NotificationPhoneMissed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/phone-paused", JSImport.Default)
  object NotificationPhonePaused extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/power", JSImport.Default)
  object NotificationPower extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/priority-high", JSImport.Default)
  object NotificationPriorityHigh extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/rv-hookup", JSImport.Default)
  object NotificationRvHookup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sd-card", JSImport.Default)
  object NotificationSdCard extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sim-card-alert", JSImport.Default)
  object NotificationSimCardAlert extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/SmsFailed", JSImport.Default)
  object SmsFailed extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sms", JSImport.Default)
  object NotificationSms extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sync-disabled", JSImport.Default)
  object NotificationSyncDisabled extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sync-problem", JSImport.Default)
  object NotificationSyncProblem extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/sync", JSImport.Default)
  object NotificationSync extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/system-update", JSImport.Default)
  object NotificationSystemUpdate extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/tap-and-play", JSImport.Default)
  object NotificationTapAndPlay extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/time-to-leave", JSImport.Default)
  object NotificationTimeToLeave extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/vibration", JSImport.Default)
  object NotificationVibration extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/voice-chat", JSImport.Default)
  object NotificationVoiceChat extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/vpn-lock", JSImport.Default)
  object NotificationVpnLock extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/wc", JSImport.Default)
  object NotificationWc extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/notification/wifi", JSImport.Default)
  object NotificationWifi extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/AcUnit", JSImport.Default)
  object PlacesAcUnit extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/airport-shuttle", JSImport.Default)
  object PlacesAirportShuttle extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/all-inclusive", JSImport.Default)
  object PlacesAllInclusive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/beach-access", JSImport.Default)
  object PlacesBeachAccess extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/business-center", JSImport.Default)
  object PlacesBusinessCenter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/casino", JSImport.Default)
  object PlacesCasino extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/child-care", JSImport.Default)
  object PlacesChildCare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/child-friendly", JSImport.Default)
  object PlacesChildFriendly extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/fitness-center", JSImport.Default)
  object PlacesFitnessCenter extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/free-breakfast", JSImport.Default)
  object PlacesFreeBreakfast extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/golf-course", JSImport.Default)
  object PlacesGolfCourse extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/hot-tub", JSImport.Default)
  object PlacesHotTub extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/kitchen", JSImport.Default)
  object PlacesKitchen extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/pool", JSImport.Default)
  object PlacesPool extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/room-service", JSImport.Default)
  object PlacesRoomService extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/rv-hookup", JSImport.Default)
  object PlacesRvHookup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/smoke-free", JSImport.Default)
  object PlacesSmokeFree extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/smoking-rooms", JSImport.Default)
  object PlacesSmokingRooms extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/places/spa", JSImport.Default)
  object PlacesSpa extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/cake", JSImport.Default)
  object SocialCake extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/domain", JSImport.Default)
  object SocialDomain extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/group-add", JSImport.Default)
  object SocialGroupAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/group", JSImport.Default)
  object SocialGroup extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/LocationCity", JSImport.Default)
  object LocationCity extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/mood-bad", JSImport.Default)
  object SocialMoodBad extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/mood", JSImport.Default)
  object SocialMood extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/notifications-active", JSImport.Default)
  object SocialNotificationsActive extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/notifications-none", JSImport.Default)
  object SocialNotificationsNone extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/notifications-off", JSImport.Default)
  object SocialNotificationsOff extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/notifications-paused", JSImport.Default)
  object SocialNotificationsPaused extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/notifications", JSImport.Default)
  object SocialNotifications extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/pages", JSImport.Default)
  object SocialPages extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/party-mode", JSImport.Default)
  object SocialPartyMode extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/people-outline", JSImport.Default)
  object SocialPeopleOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/people", JSImport.Default)
  object SocialPeople extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/person-add", JSImport.Default)
  object SocialPersonAdd extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/person-outline", JSImport.Default)
  object SocialPersonOutline extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/person", JSImport.Default)
  object SocialPerson extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/plus-one", JSImport.Default)
  object SocialPlusOne extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/poll", JSImport.Default)
  object SocialPoll extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/public", JSImport.Default)
  object SocialPublic extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/school", JSImport.Default)
  object SocialSchool extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/sentiment-dissatisfied", JSImport.Default)
  object SocialSentimentDissatisfied extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/sentiment-neutral", JSImport.Default)
  object SocialSentimentNeutral extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/sentiment-satisfied", JSImport.Default)
  object SocialSentimentSatisfied extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/sentiment-very-dissatisfied", JSImport.Default)
  object SocialSentimentVeryDissatisfied extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/sentiment-very-satisfied", JSImport.Default)
  object SocialSentimentVerySatisfied extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/social/share", JSImport.Default)
  object SocialShare extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/Whatshot", JSImport.Default)
  object Whatshot extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/check-box-outline-blank", JSImport.Default)
  object ToggleCheckBoxOutlineBlank extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/check-box", JSImport.Default)
  object ToggleCheckBox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/indeterminate-check-box", JSImport.Default)
  object ToggleIndeterminateCheckBox extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/radio-button-checked", JSImport.Default)
  object ToggleRadioButtonChecked extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/radio-button-unchecked", JSImport.Default)
  object ToggleRadioButtonUnchecked extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/star-border", JSImport.Default)
  object ToggleStarBorder extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/star-half", JSImport.Default)
  object ToggleStarHalf extends MuiSvgIcon
  @js.native @JSImport("@material-ui/icons/toggle/star", JSImport.Default)
  object ToggleStar extends MuiSvgIcon
}
