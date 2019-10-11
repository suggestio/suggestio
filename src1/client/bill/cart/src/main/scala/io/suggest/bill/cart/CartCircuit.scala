package io.suggest.bill.cart

import diode.react.ReactConnector
import io.suggest.bill.cart.c.{BillConfAh, ILkCartApi, LkCartApiXhrImpl, OrderItemsAh}
import io.suggest.bill.cart.m.{LoadCurrentOrder, MCartRootS, MOrderItemsS}
import io.suggest.bill.cart.u.CartUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.StateInp
import play.api.libs.json.Json
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 10:57
  * Description: Ядро компонента react-корзины биллинга.
  */
object CartCircuit {

  /** Получение модели из инпута на странице.
    * @return Инстанс MCartRootS.
    */
  def mkInitialModelFromInput(): MCartRootS = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json
      .parse(json)
      .as[MCartInit]

    // Сборка root-модели, готовой к работе.
    MCartRootS(
      conf = minit.conf,
      order = MOrderItemsS(
        jdRuntime = CartUtil.mkJdRuntime()
      )
    )
  }

}


/** Абстрактный Circuit под разные варианты использования.
  * Например, на лк-странице корзины надо initialModel брать из страницы,
  * а в выдаче - брать конфиг откуда-то сверху.
  */
abstract class CartCircuitBase
  extends CircuitLog[MCartRootS]
  with ReactConnector[MCartRootS]
{

  import io.suggest.bill.cart.m.MOrderItemsS.MOrderItemsSFastEq

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.CART_CIRCUIT_ERROR


  // Модели
  //private lazy val rootRO = zoom(identity)
  private val orderRW = zoomRW(_.order)(_.withOrder(_))
  private val confRW = zoomRW(_.conf)(_.withConf(_))


  // server-API для контроллеров.
  val lkCartApi: ILkCartApi = new LkCartApiXhrImpl


  // Контроллеры
  private val orderItemsAh = new OrderItemsAh(
    lkCartApi   = lkCartApi,
    modelRW     = orderRW
  )

  private val billConfAh = new BillConfAh(
    modelRW = confRW
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      orderItemsAh,
      billConfAh
    )
  }


  // В конце инициализации - запустить к серверу запрос о текущем ордере.
  Future {
    dispatch( LoadCurrentOrder )
  }

}


/** Circuit для html-страницы-ресурса с корзиной. */
class CartPageCircuit extends CartCircuitBase {

  override protected def initialModel: MCartRootS =
    CartCircuit.mkInitialModelFromInput()

}
