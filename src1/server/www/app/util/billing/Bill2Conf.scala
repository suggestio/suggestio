package util.billing

import akka.actor.ActorSystem
import io.suggest.bill.{MCurrencies, MGetPriceResp, MPrice}
import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import io.suggest.util.logs.MacroLogsDyn
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.18 14:29
  * Description: Модуль констант биллинга. Вынесена за пределы [[Bill2Util]], т.к. нужна в разнах модулях.
  */
@Singleton
class Bill2Conf @Inject() (
                            injector                  : Injector,
                          )
  extends MacroLogsDyn
{

  implicit private def ec = injector.instanceOf[ExecutionContext]
  private def mNodes = injector.instanceOf[MNodes]
  private def _esModel = injector.instanceOf[EsModel]
  private def actorSystem = injector.instanceOf[ActorSystem]
  private def configuration = injector.instanceOf[Configuration]

  /** id узла, на который должна сыпаться комиссия с этого биллинга. */
  lazy val CBCA_NODE_ID: String = {
    val ck = "bill.cbca.node.id"
    val res = configuration
      .getOptional[String](ck)
      .getOrElse {
        val r = "-vr-hrgNRd6noyQ3_teu_A"
        LOGGER.trace("CBCA node id defaulted to " + r)
        r
      }

    val esModel = _esModel
    import esModel.api._

    // Проверить в фоне, существует ли узел. Если нет, то в системе какая-то неисправность, и надо привлечь к ней внимание.
    for {
      _ <- mNodes.getByIdCache(res)
        .map(_.get)
        .failed
    } {
      // Что-то пошло не так, надо застрелиться, ругнувшись в логи.
      LOGGER.error(s"CBCA NODE[$res] IS MISSING! Billing will work wrong, giving up. Check conf.key: $ck")
      actorSystem.terminate()
    }

    // Вернуть id узла.
    res
  }


  /** Нулевая цена. */
  def zeroPrice: MPrice = {
    MPrice(0L, MCurrencies.default)
  }

  /** Нулевой прайсинг размещения. */
  def zeroPricing: MGetPriceResp = {
    val prices = zeroPrice :: Nil
    MGetPriceResp(prices)
  }

  def zeroPricingFut = Future.successful( zeroPricing )

  /**
    * У админов есть бесплатное размещение.
    * Код обработки бесплатного размещения вынесен сюда.
    */
  def maybeFreePricing(forceFree: Boolean)(f: => Future[MGetPriceResp]): Future[MGetPriceResp] = {
    if (forceFree)
      zeroPricingFut
    else
      f
  }

}
