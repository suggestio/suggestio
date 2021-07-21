package io.suggest.sys.mdr

import diode.react.ReactConnector
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.m.{MJdArgs, MJdRuntime}
import io.suggest.msg.ErrorMsgs
import io.suggest.log.CircuitLog
import io.suggest.sys.mdr.c.{ISysMdrApi, NodeMdrAh, SysMdrApiXhrImpl}
import io.suggest.sys.mdr.m.{MMdrNodeS, MSysMdrRootS, MdrNextNode}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.{CircuitUtil, StateInp}
import io.suggest.sys.mdr.u.SysMdrUtil
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
  val nodeRW = CircuitUtil.mkLensRootZoomRW(this, MSysMdrRootS.node)
  val jdArgsOptRW = CircuitUtil.mkLensZoomRW( nodeRW, MMdrNodeS.jdArgsOpt )

  val jdAh = new JdAh(
    // JdAh not purely fits into sys-mdr form logic. Let'a add some ephemeral crunches...
    modelRW = jdArgsOptRW.zoomRW[MJdRuntime] { jdArgsOpt =>
      jdArgsOpt.fold( SysMdrUtil.mkJdRuntime(LazyList.empty) )( _.jdRuntime )
    } { (jdArgsOpt0, jdRuntime2) =>
      jdArgsOpt0.map( MJdArgs.jdRuntime.set(jdRuntime2) )
    },
  )

  // Controllers
  val nodeMdrAh = new NodeMdrAh(
    api     = sysMdrApi,
    modelRW = rootRW
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      nodeMdrAh,
      jdAh,
    )
  }

  // После конструктора, запустить получение начальных данных с сервера:
  Future {
    dispatch( MdrNextNode() )
  }

}
