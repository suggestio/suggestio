package controllers.sc

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sc.MScApiVsns
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.MScQs
import models.req.IReq
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


  /** Необходимо ли перевести focused-запрос в index запрос? */
  protected def _isFocGoToProducerIndexFut(qs: MScQs): Future[Option[MNode]] = {
    lazy val logPrefix = s"_isFocGoToProducerIndexFut()#${System.currentTimeMillis()}:"

    val toProducerFutOpt = for {
      focQs <- qs.foc
      if focQs.focIndexAllowed
    } yield {
      for {
        // Прочитать из хранилища указанную карточку.
        madOpt <- mNodesCache.getById( focQs.lookupAdId )

        // .get приведёт к NSEE, это нормально.
        producerId = {
          madOpt
            .flatMap(n2NodesUtil.madProducerId)
            .get
        }

        if !(qs.search.rcvrId containsStr producerId )

        producer <- {
          // 2015.sep.16: Нельзя перепрыгивать на продьюсера, у которого не больше одной карточки на главном экране.
          val prodAdsCountFut: Future[Long] = {
            val args = new MNodeSearchDfltImpl {
              override def outEdges: Seq[ICriteria] = {
                val cr = Criteria(
                  predicates  = MPredicates.Receiver :: Nil,
                  nodeIds     = producerId :: Nil
                )
                cr :: Nil
              }
              override def limit = 2
              override def nodeTypes = MNodeTypes.Ad :: Nil
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

      } yield {
        // Да, вернуть продьюсера.
        LOGGER.trace(s"$logPrefix Foc ad#${focQs.lookupAdId} go-to index producer#$producerId")
        Some( producer )
      }
    }

    toProducerFutOpt
      .getOrElse( Future.failed(new NoSuchElementException) )
      .recover { case _: NoSuchElementException =>
        None
      }
  }


  /** Тело экшена возврата медиа-кнопок расширено поддержкой переключения на index-выдачу узла-продьюсера
    * рекламной карточки, которая заинтересовала юзера. */
  override protected def _focusedAds(qs: MScQs)(implicit request: IReq[_]): Future[Result] = {
    val resFut = for {
      producerOpt <- _isFocGoToProducerIndexFut(qs)
      producer = producerOpt.get
      majorApiVsn = qs.common.apiVsn.majorVsn
      idxLogic: ScIndexLogic = if (majorApiVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        ScFocToIndexLogicV3(producer, qs)(request)
      } else {
        throw new IllegalArgumentException("Unknown API: " + majorApiVsn)
      }
      result <- idxLogic.resultFut
    } yield {
      result
    }

    // Отрабатываем все возможные ошибки через вызов к super-методу.
    resFut.recoverWith { case ex: Throwable =>
      // Продьюсер неизвестен, не подходит под критерии или переброска отключена.
      if ( !ex.isInstanceOf[NoSuchElementException] )
        LOGGER.error("_focusedAds(): Suppressing unexpected exception for " + request.uri, ex)
      super._focusedAds(qs)
    }
  }


  /** ScIndex-логика перехода на с focused-карточки в индекс выдачи продьюсера фокусируемой карточки.
    *
    * @param producer Продьюсер, в который требуется перескочить.
    * @param focQs Исходные qs-аргументы запроса фокусировки.
    * @param _request Исходный HTTP-реквест.
    */
  case class ScFocToIndexLogicV3(producer: MNode, focQs: MScQs)
                                (override implicit val _request: IReq[_]) extends ScIndexLogic {

    /** Подстановка qs-аргументы реквеста. */
    // TODO Заменить lazy val на val.
    override lazy val _qs: MScQs = {
      // v3 выдача. Собрать аргументы для вызова index-логики:
      MScQs(
        common = focQs.common,
        index = Some(
          MScIndexArgs(
            withWelcome = true
          )
        )
      )
    }

    override def isFocusedAdOpen = true

    override lazy val indexNodeFut: Future[MIndexNodeInfo] = {
      val nodeInfo = MIndexNodeInfo(
        mnode   = producer,
        isRcvr  = true
      )
      Future.successful( nodeInfo )
    }

  }

}
