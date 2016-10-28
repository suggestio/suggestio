package io.suggest.sc.sjs.c.plat

import _root_.cordova.Cordova
import io.suggest.sc.sjs.m.mdev.MPlatFsmSd
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 11:49
  * Description: Реализация FSM, который скрывает взаимодействия с платформенной частью.
  */
object PlatformFsm
  extends dev.Browser
  with dev.Cordova
  with ScSjsFsmLogger
{

  override protected var _stateData = MPlatFsmSd()
  override protected var _state: FsmState = new DummyState


  /** Запуск этого FSM в работу. */
  def start(): Unit = {
    val st0 = if ( !js.isUndefined(Cordova) ) {
      new CordovaS
    } else {
      new BrowserS
    }
    become(st0)
  }


  // ---------------------------------------------------
  // States

  private class DummyState extends FsmEmptyReceiverState

  /** Взаимодействие с браузером. */
  protected class BrowserS extends BrowserStateT

  /** Взаимодействие с cordova webview. */
  protected class CordovaS extends CordovaStateT

}
