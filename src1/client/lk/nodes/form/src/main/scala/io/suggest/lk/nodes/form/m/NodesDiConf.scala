package io.suggest.lk.nodes.form.m

import diode.ActionResult
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.proto.http.model.IMHttpClientConfig
import io.suggest.spa.SioPages
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.2020 21:00
  * Description: Модель конфигурации LkNodesForm, которая происходит в модуле.
  */
trait NodesDiConf extends IMHttpClientConfig {

  /** Начальные данные для инициализации nodes circuit. */
  def circuitInit(): ActionResult[MLkNodesRoot]

  /** Роутер выдачи, если форма встроена в выдачу. */
  def scRouterCtlOpt: Option[RouterCtl[SioPages.Sc3]]

  /** Если форма способна закрываться, то тут callback. */
  def closeForm: Option[Callback]

  /** Рендерить ли ссылки на личный кабинет? */
  def showLkLinks: Boolean

}


object NodesDiConf {

  /** DI-конфиг формы, живущей на страницах личного кабинета. */
  object LkConf extends NodesDiConf {
    override def circuitInit() = LkNodesFormCircuit.initIsolated()
    override def scRouterCtlOpt = None
    override def closeForm = None
    override def showLkLinks = true
  }

}