package controllers.sc

import io.suggest.n2.node.MNode
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.MScQs
import models.req.IReq
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
          )
        )
      )
    }

    override def isFocusedAdOpen = {
      true
    }

    override lazy val indexNodesFut: Future[Seq[MIndexNodeInfo]] = {
      val nodeInfo = MIndexNodeInfo(
        mnode   = producer,
        isRcvr  = true
      )
      Future.successful( nodeInfo :: Nil )
    }

  }

}
