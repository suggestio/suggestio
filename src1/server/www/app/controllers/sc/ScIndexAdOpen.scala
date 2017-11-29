package controllers.sc

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.req.IReq
import models.im.DevScreen
import models.msc._
import play.api.mvc.Result
import util.n2u.IN2NodesUtilDi
import japgolly.univeq._

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
  with ScIndex
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
        if (logic._qs.focJumpAllowed) {
          Future.successful(None)
        } else {
          val ex = new NoSuchElementException("Foc jump disabled by sc-sjs.")
          Future.failed(ex)
        }
      }

      // Прочитать из хранилища указанную карточку.
      madOpt <- mNodesCache.getById( logic._qs.lookupAdId )

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
          val args = new MNodeSearchDfltImpl {
            override def outEdges: Seq[ICriteria] = {
              val cr = Criteria(
                predicates  = MPredicates.Receiver :: Nil,
                nodeIds     = producerId :: Nil
              )
              Seq(cr)
            }
            override def limit = 2
            override def nodeTypes = Seq( MNodeTypes.Ad )
          }
          mNodes.dynCount(args)
        }

        val prodFut = mNodesCache.getById(producerId)
          .map(_.get)
        // Как выяснилось, бывают карточки-сироты (продьюсер удален, карточка -- нет). Нужно сообщать об этой ошибке.
        for (ex <- prodFut.failed) {
          val msg = s"Producer node[$producerId] does not exist."
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
    val majorApiVsn = focLogic._qs.apiVsn.majorVsn

    // TODO Надо дедублицировать тут код как-то... Нужно изобретать wrapper-trait?
    val idxLogic: ScIndexLogic = if (majorApiVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
      new ScIndexLogicV3 {
        override def isFocusedAdOpen    = true
        override def _syncArgs          = MScIndexSyncArgs.empty
        override lazy val indexNodeFut: Future[MIndexNodeInfo] = {
          Future.successful(
            MIndexNodeInfo(
              mnode   = producer,
              isRcvr  = true
            )
          )
        }
        override def _request  = request
        override def _reqArgs: MScIndexArgs = new MScIndexArgsDfltImpl {
          private val s = focLogic._qs
          override def prevAdnId: Option[String]  = {
            s.search
              .rcvrIdOpt
              .map(_.id)
          }
          override def screen: Option[DevScreen]  = s.screen
          override def apiVsn: MScApiVsn          = s.apiVsn
          override def withWelcome                = true
          override def locEnv                     = s.search.locEnv
        }
      }

    } else if (majorApiVsn ==* MScApiVsns.Sjs1.majorVsn) {
      new ScIndexLogicV2 {
        override def _syncArgs          = MScIndexSyncArgs.empty
        override lazy val indexNodeFut: Future[MIndexNodeInfo] = {
          Future.successful(
            MIndexNodeInfo(
              mnode   = producer,
              isRcvr  = true
            )
          )
        }
        override def _request  = request
        override def _reqArgs: MScIndexArgs = new MScIndexArgsDfltImpl {
          private val s = focLogic._qs
          override def prevAdnId: Option[String]  = {
            s.search
              .rcvrIdOpt
              .map(_.id)
          }
          override def screen: Option[DevScreen]  = s.screen
          override def apiVsn: MScApiVsn          = s.apiVsn
          override def withWelcome                = true
          override def locEnv                     = s.search.locEnv
        }
      }

    } else {
      throw new IllegalArgumentException("Unknown API: " + majorApiVsn)
    }

    // Логика обработки запроса собрана, запустить исполнение запроса.
    idxLogic.result
  }

}
