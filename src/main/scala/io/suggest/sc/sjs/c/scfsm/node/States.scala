package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.sjs.c.scfsm.GridAppend

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 16:36
 * Description: Фасад пакета scfms.node. Здесь содержаться полуготовые состояния цепочки инициализации узла.
 */
trait States extends Index with Welcome with GridAppend {

  /** Трейт состояния запуска запроса index и ожидания его исполнения. */
  trait NodeInit_GetIndex_WaitIndex_StateT
    extends GetIndexStateT
    with WaitIndexStateT

  /** Трейт состояния нахождения на ещё-нескрывающейся welcome-карточке с ожиданием ответа сервера по Grid ads. */
  trait NodeInit_WelcomeShowing_GridAdsWait_StateT
    extends OnWelcomeShownStateT
    with GridAdsWaitLoadStateT
  {
    // Анимация не требуется, т.к. welcome-карточка непрозрачно закрывает всё собой.
    override protected def _gridAppendAnimated = false
  }

  /** Трейт состояния, когда welcome-карточка отображается статично, а карточки от сервера уже получены. */
  trait NodeInit_WelcomeShowing_StateT
    extends OnWelcomeShownStateT


  /** Трейт состояния, когда welcome-карточка начала скрываться, а grid-карточки с сервера ещё не пришли. */
  trait NodeInit_WelcomeHiding_GridAdsWait_StateT
    extends OnWelcomeHidingState
    with GridAdsWaitLoadStateT


  /** Трейт состояния, когда welcome-карточка уже скрылась полностью, а grid-карточки ещё от сервера не получены. */
  trait NodeInit_GridAdsWait_StateT
    extends GridAdsWaitLoadStateT

  /** Трейт состояния, когда карточки выдачи уже получены, а welcome-карточка скрывается. */
  trait NodeInit_WelcomeHiding_StateT
    extends OnWelcomeHidingState


  /** Трейт состояния, когда возникла ошибка запроса grid ads до начала сокрытия welcome-карточки. */
  trait NodeInit_WelcomeShowing_GridAdsFailed_StateT
    extends OnWelcomeShownStateT
    with GetGridAdsStateT
    with GridAdsWaitLoadStateT

  /** Трейт состояния, когда возникла ошибка запроса grid ads во время сокрытия welcome-карточки. */
  trait NodeInit_WelcomeHiding_GridAdsFailed_StateT
    extends OnWelcomeHidingState
    with GetGridAdsStateT
    with GridAdsWaitLoadStateT

  /** Трейт состояния, когда возникла ошибка после полного сокрытия welcome. */
  trait NodeInit_GridAdsFailed_StateT
    extends GetGridAdsStateT
    with GridAdsWaitLoadStateT

}
