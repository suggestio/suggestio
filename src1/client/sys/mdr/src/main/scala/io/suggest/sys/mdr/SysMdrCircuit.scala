package io.suggest.sys.mdr

import diode.react.ReactConnector
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sys.mdr.c.{ISysMdrApi, NodeMdrAh, SysMdrApiXhrImpl}
import io.suggest.sys.mdr.m.{MSysMdrRootS, MdrNextNode}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.StateInp
import io.suggest.sys.mdr.v.main.NodeRenderR
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:01
  * Description: Цепь компонента модерации карточек.
  */
object SysMdrCircuit {

  /** Сборка начального состояния по данным из DOM. */
  def getInitModelFromDom(): MSysMdrRootS = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val mconf = Json
      .parse(json)
      .as[MMdrConf]

    // Сборка root-модели, готовой к работе.
    MSysMdrRootS(
      jdCss = NodeRenderR.mkJdCss()(),
      conf  = mconf,
    )
  }

}


class SysMdrCircuit extends CircuitLog[MSysMdrRootS] with ReactConnector[MSysMdrRootS] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.SYS_MDR_CIRCUIT_ERROR

  override protected def initialModel = SysMdrCircuit.getInitModelFromDom()


  // API
  val sysMdrApi: ISysMdrApi = new SysMdrApiXhrImpl


  // Model
  val rootRW = zoomRW[MSysMdrRootS](identity)((_, v2) => v2)


  // Controllers
  val nodeMdrAh = new NodeMdrAh(
    api     = sysMdrApi,
    modelRW = rootRW
  )

  override protected def actionHandler: HandlerFunction = {
    nodeMdrAh
  }

  // После конструктора, запустить получение начальных данных с сервера:
  Future {
    dispatch( MdrNextNode() )
  }

}
