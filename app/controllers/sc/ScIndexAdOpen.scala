package controllers.sc

import models.{MAdnNode, MAdnNodeCache}
import models.im.DevScreen
import models.msc.{MScApiVsn, ScReqArgsDflt, ScReqArgs}
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
  private val STEP_INTO_FOREIGN_SC_ENABLED = configuration.getBoolean("sc.focus.step.into.foreign.sc.enabled") getOrElse true


  /** Тело экшена возврата медиа-кнопок расширено поддержкой переключения на index-выдачу узла-продьюсера
    * рекламной карточки, которая заинтересовала юзера. */
  override protected def _focusedAds(logic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // 2015.jun.2: При выборе продьюсера в списке магазинов надо делать переход в выдачу магазина.
    val fut0 = if (STEP_INTO_FOREIGN_SC_ENABLED)
      Future successful None
    else
      Future failed new NoSuchElementException()
    fut0.map { _ =>
      logic._adSearch
        .producerIds
        .headOption
    }.flatMap {
      case None =>
        // Если нет id продьюсера в исходном запросе, то попробовать поискать в запрошенной first-id карточке
        logic.firstAdsFut map { firstAds =>
          firstAds
            .headOption
            .filter { _ => STEP_INTO_FOREIGN_SC_ENABLED }
            .map { _.producerId }
        }
      case some =>
        Future successful some
    }.filter { prodIdOpt =>
      // Для переброски на index допускаются только сторонние продьюсеры
      prodIdOpt.exists( logic.is3rdPartyProducer )
    }.flatMap { prodIdOpt =>
      // Для этого продьюсера нужно выполнить переброску в index.
      val fut = MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
        .map { _.get }
        .flatMap { producer => _goToProducerIndex(producer, logic) }
      // Как выяснилось, бывают карточки-сироты (продьюсер удален, карточка -- нет). Нужно сообщать об этой ошибке.
      fut onFailure { case ex =>
        val msg = s"Producer node does not exist: adnId=$prodIdOpt for adIds=${logic._adSearch.forceFirstIds}"
        if (ex.isInstanceOf[NoSuchElementException])
          LOGGER.error(msg)
        else
          LOGGER.error(msg, ex)
      }
      fut
    }.recoverWith { case ex: Throwable =>
      // Продьюсер неизвестен, не подходит под критерии или переброска отключена.
      if (!ex.isInstanceOf[NoSuchElementException])
        LOGGER.error("_focusedAds(): Suppressing unexpected exception for " + logic._request.uri, ex)
      super._focusedAds(logic)
    }
  }


  /**
   * Решено, что юзера нужно перебросить на выдачу другого узла с возможностью возрата на исходный узел
   * через кнопку навигации.
   * @param producer Узел-продьюсер, на который необходимо переключиться.
   * @param focLogic Закешированная focused-логика, собранная в экшене.
   * @param request Исходный реквест.
   * @return Фьючерс с http-результатом.
   */
  private def _goToProducerIndex(producer: MAdnNode, focLogic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Извлекаем MAdnNode втупую. exception будет перехвачен в recoverWith.
    val idxLogic = new ScIndexNodeSimpleHelper {
      override def geoListGoBackFut   = Future successful Some(true)
      override def adnNodeFut         = Future successful producer
      override def isGeo              = false
      override implicit def _request  = request
      override def _reqArgs: ScReqArgs = new ScReqArgsDflt {
        private val s = focLogic._adSearch
        override def prevAdnId: Option[String]  = s.receiverIds.headOption
        override def screen: Option[DevScreen]  = s.screen
        override def apiVsn: MScApiVsn          = s.apiVsn
        override def withWelcomeAd              = true
        override def geo                        = s.geo
      }

    }
    idxLogic.result
  }

}
