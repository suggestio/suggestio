package controllers.sc

import models.MAdnNodeCache
import models.msc.ScReqArgs
import play.api.mvc.Result
import util.acl.AbstractRequestWithPwOpt
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.05.15 14:27
 * Description: Поддержка переключения узла выдачи при щелчке на размещенную карточку.
 * По сути тут комбинация из index и focused логик.
 */
trait ScIndexAdOpen extends ScFocusedAds with ScIndexNodeCommon {

  /** Активирован ли автопереход в выдачу узла-продьюсера размещенной на данном узле рекламной карточки? */
  // TODO Выпилить начисто, когда будет запилена нормальная поддержка функции тут и на клиенте.
  private val STEP_INTO_FOREIGN_SC_ENABLED = configuration.getBoolean("sc.focus.step.into.foreign.sc.enabled") getOrElse false


  /** Тело экшена возврата медиа-кнопок расширено поддержкой переключения на index-выдачу узла-продьюсера
    * рекламной карточки, которая заинтересовала юзера. */
  override protected def _focusedAds(logic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // 2015.may.8: При выборе чужой карточки нужно делать переход в выдачу чуждого узла с возможностью возврата.
    logic.firstAdsFut flatMap { firstAds =>
      val stepToProdAdnIdOpt = firstAds
        .headOption
        .filter { _ => STEP_INTO_FOREIGN_SC_ENABLED }
        .map { _.producerId }
        .filter { !logic._adSearch.receiverIds.contains(_) }
      stepToProdAdnIdOpt match {
        case Some(prodId) =>
          _goToProducerIndex(prodId, logic)
        case None =>
          super._focusedAds(logic)
      }
    }
  }


  /**
   * Решено, что юзера нужно перебросить на выдачу другого узла с возможностью возрата на исходный узел
   * через кнопку навигации.
   * @param adnId id узла.
   * @param focLogic Закешированная focused-логика, собранная в экшене.
   * @param request Исходный реквест.
   * @return Фьючерс с http-результатом.
   */
  private def _goToProducerIndex(adnId: String, focLogic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Извлекаем MAdnNode втупую. exception будет перехвачен в recoverWith.
    val mnodeFut = MAdnNodeCache.getById(adnId)
        .map { _.get }
    val idxLogic = new ScIndexNodeSimpleHelper {
      override def geoListGoBackFut   = Future successful Some(true)
      override def adnNodeFut         = mnodeFut
      override def isGeo              = false
      override implicit def _request  = request
      override def _reqArgs           = ScReqArgs.empty   // TODO Stub. ScIndexAdOpen скорее всего будет выкинут, поэтому реализовывать это не требуется.
    }
    idxLogic.result
      // Should never happen.
      .recoverWith { case ex =>
        LOGGER.warn("Failed to make node on-ad-open switch, because producer node does not exists: " + adnId, ex)
        _focusedAds(focLogic)
      }
  }

}
