package io.suggest.lk.nodes.form.m

import diode.{ActionResult, Effect}
import io.suggest.dev.MOsFamily
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.nfc.INfcApi
import io.suggest.nfc.web.WebNfcApi
import io.suggest.proto.http.model.IMHttpClientConfig
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.2020 21:00
  * Description: Модель конфигурации LkNodesForm, которая происходит в модуле.
  */
trait NodesDiConf extends IMHttpClientConfig {

  /** Начальные данные для инициализации nodes circuit. */
  def circuitInit(): ActionResult[MLkNodesRoot]

  /** Роутер выдачи, если форма встроена в выдачу.
    * fun: $1 - nodeId => Callback-эффект
    */
  def openNodeScOpt: Option[String => Callback]

  /** Если форма способна закрываться, то тут callback. */
  def closeForm: Option[Callback]

  /** Рендерить ли ссылки на личный кабинет? */
  def showLkLinks(): Boolean

  /** Проверка залогиненности юзера. В выдаче (в т.ч. приложении) юзер может быть без логина. */
  def isUserLoggedIn(): Boolean

  /** Вёрстка для рендара сообщения о необходимости залогиниться. */
  def needLogInVdom(chs: VdomNode*): VdomNode

  /** Надо ли инициализировать части, связанные с beacons-сканнером? */
  def withBleBeacons: Boolean

  /** Трансформер эффекта, который используется для подавления ошибки.
    * Для sc3 требуется проверить валидность CSRF-токена, например.
    */
  def retryErrorFx(ex: Throwable, effect: Effect): Effect

  def onErrorFxOpt: Option[Effect]

  /** Define NFC API for use. */
  def nfcApi: Option[INfcApi]

  /** Nodes form running in context of this ad node id.
    * This differs from conf.adId, because None may be configured, but some adId still presents outside and
    * may be used in non-ad Nodes-form mode. */
  def contextAdId(): Option[String]

  /** If browser - must be None.
    * If cordova - return current device operating system family.
    */
  def appOsFamily: Option[MOsFamily]

}


object NodesDiConf {

  /** DI-конфиг формы, живущей на страницах личного кабинета. */
  object LkConf extends NodesDiConf {
    override def circuitInit() = LkNodesFormCircuit.initIsolated()
    override def openNodeScOpt = None
    override def closeForm = None
    override def showLkLinks() = true
    override def isUserLoggedIn() = true
    override def needLogInVdom(chs: VdomNode*) = EmptyVdom
    override def withBleBeacons = false
    override def onErrorFxOpt = None
    override def retryErrorFx(ex: Throwable, effect: Effect) = effect
    override lazy val nfcApi = {
      // Standard WebNFC API will is tried automatically.
      val webNfc = new WebNfcApi
      Option.when( webNfc.isApiAvailable() )( webNfc )
    }
    override def contextAdId() = None
    override def appOsFamily = None
  }

}
