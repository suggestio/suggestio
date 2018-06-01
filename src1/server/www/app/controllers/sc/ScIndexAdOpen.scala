package controllers.sc

import io.suggest.model.n2.node.MNode
import io.suggest.sc.MScApiVsns
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.MScQs
import japgolly.univeq._
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import util.showcase.IScUtil

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
  with IScUtil
{

  import mCommonDi._


  /** Тело экшена возврата медиа-кнопок расширено поддержкой переключения на index-выдачу узла-продьюсера
    * рекламной карточки, которая заинтересовала юзера. */
  override protected def _focusedAds(qs: MScQs)(implicit request: IReq[_]): Future[Result] = {
    val resFut = for {
      producerOpt <- scUtil.isFocGoToProducerIndexFut(qs)
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
