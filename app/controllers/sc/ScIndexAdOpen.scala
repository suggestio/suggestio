package controllers.sc

import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.IMNodes
import models.req.IReq
import models.{AdSearchImpl, AdShowLevels, MNode}
import models.im.DevScreen
import models.msc.{MScApiVsn, ScReqArgs, ScReqArgsDfltImpl}
import play.api.mvc.Result
import util.n2u.IN2NodesUtilDi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.05.15 14:27
 * Description: Поддержка переключения узла выдачи при щелчке на размещенную карточку.
 * По сути тут комбинация из index и focused логик.
 */
trait ScIndexAdOpen
  extends ScFocusedAds
  with ScIndexNodeCommon
  with IN2NodesUtilDi
  with IMNodes
{
  import mCommonDi._

  /** Тело экшена возврата медиа-кнопок расширено поддержкой переключения на index-выдачу узла-продьюсера
    * рекламной карточки, которая заинтересовала юзера. */
  override protected def _focusedAds(logic: FocusedAdsLogicHttp): Future[Result] = {
    import logic._request

    val resFut = for {
      // Фильтруем по флагу focJumpAllowed. if в первой строчке foc{} использовать нельзя, поэтому имитируем тут Future.
      _ <- {
        if (logic._adSearch.focJumpAllowed) {
          Future.successful(None)
        } else {
          val ex = new NoSuchElementException("Foc jump disabled by sc-sjs.")
          Future.failed(ex)
        }
      }

      // Прочитать из хранилища указанную карточку.
      madOpt <- mNodeCache.getById( logic._adSearch.lookupAdId )

      // .get приведёт к NSEE, это нормально.
      producerId = {
        madOpt
          .flatMap(n2NodesUtil.madProducerId)
          .get
      }

      if logic.is3rdPartyProducer( producerId )

      producer <- {
        // 2015.sep.16: Нельзя перепрыгивать на продьюсера, у которого не больше одной карточки на главном экране.
        val prodAdsCountFut: Future[Long] = {
          val args = new AdSearchImpl {
            override def outEdges: Seq[ICriteria] = {
              val cr = Criteria(
                nodeIds = Seq(producerId),
                sls     = Seq(AdShowLevels.LVL_START_PAGE)
              )
              Seq(cr)
            }
            override def limit        = 2
          }
          mNodes.dynCount(args)
        }

        val prodFut = mNodeCache.getById(producerId)
          .map(_.get)
        // Как выяснилось, бывают карточки-сироты (продьюсер удален, карточка -- нет). Нужно сообщать об этой ошибке.
        prodFut.onFailure { case ex =>
          val msg = s"Producer node does not exist: adnId=$producerId for adIds=${logic._adSearch.firstIds}"
          if (ex.isInstanceOf[NoSuchElementException])
            LOGGER.error(msg)
          else
            LOGGER.error(msg, ex)
        }
        // Фильтруем prodFut по кол-ву карточек, размещенных у него на главном экране.
        prodAdsCountFut
          .filter { _ > 1 }
          .flatMap { _ => prodFut }
      }

      result <- _goToProducerIndex(producer, logic)

    } yield {
      result
    }

    // Отрабатываем все возможные ошибки через вызов к super-методу.
    resFut.recoverWith { case ex: Throwable =>
      // Продьюсер неизвестен, не подходит под критерии или переброска отключена.
      if ( !ex.isInstanceOf[NoSuchElementException] )
        LOGGER.error("_focusedAds(): Suppressing unexpected exception for " + logic._request.uri, ex)
      super._focusedAds(logic)
    }
  }


  /**
   * Решено, что юзера нужно перебросить на выдачу другого узла с возможностью возрата на исходный узел
   * через кнопку навигации.
    *
    * @param producer Узел-продьюсер, на который необходимо переключиться.
   * @param focLogic Закешированная focused-логика, собранная в экшене.
   * @param request Исходный реквест.
   * @return Фьючерс с http-результатом.
   */
  private def _goToProducerIndex(producer: MNode, focLogic: FocusedAdsLogicHttp)
                                (implicit request: IReq[_]): Future[Result] = {
    // Извлекаем MAdnNode втупую. exception будет перехвачен в recoverWith.
    val idxLogic = new ScIndexNodeHelper {
      override def geoListGoBackFut   = Future.successful( Some(true) )
      override def adnNodeFut         = Future.successful( producer )
      override implicit def _request  = request
      override def _reqArgs: ScReqArgs = new ScReqArgsDfltImpl {
        private val s = focLogic._adSearch
        override def prevAdnId: Option[String]  = {
          n2NodesUtil.receiverIds(s.outEdges)
            .toStream
            .headOption
        }
        override def screen: Option[DevScreen]  = s.screen
        override def apiVsn: MScApiVsn          = s.apiVsn
        override def withWelcomeAd              = true
        override def geo                        = s.geo
      }
    }
    // Логика обработки запроса собрана, запустить исполнение запроса.
    idxLogic.result
  }

}
