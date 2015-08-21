package io.suggest.sc.sjs.c.scfsm.foc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 18:40
 * Description: Гибридные состояния touch-шифтинга и идущего в фоне preload'а карточек.
 * Они появляются путём банального смешивания трейтов.
 */
trait OnTouchPreload extends OnTouch with PreLoading {

  protected trait OnTouchShiftPreloadRightStateT
    extends ForRightPreLoadingReceiveStateT
    with FocOnTouchShiftStateT

  protected trait OnTouchShiftPreloadLeftStateT
    extends ForLeftPreLoadingReceiveStateT
    with FocOnTouchShiftStateT

}
