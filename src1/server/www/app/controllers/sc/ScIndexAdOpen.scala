package controllers.sc

import io.suggest.geo.MLocEnv
import io.suggest.n2.node.MNode
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
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
      var qsCommon2 = focQs.common

      // Если выставлен отказ от bluetooth-маячков в ответе, то убрать маячки из locEnv.
      if (
        focQs.foc
          .exists(_.indexAdOpen
            .exists(!_.withBleBeaconAds)) &&
        qsCommon2.locEnv.bleBeacons.nonEmpty
      ) {
        qsCommon2 = MScCommonQs.locEnv
          .composeLens( MLocEnv.bleBeacons )
          .set( Nil )( qsCommon2 )
      }

      MScQs(
        common = qsCommon2,
        index = Some(
          MScIndexArgs(
          )
        ),
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
