package io.suggest.lk.m

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:35
  * Description: Diode-экшены для нужд lk-common-sjs.
  */

sealed trait ILkCommonAction extends DAction
sealed trait ILkCommonPopupCloseAction extends ILkCommonAction
/** Трейт-маркер закрытия "бесполезного" попапа, т.к. не содержащего ожидаемый контект, а какую-то малополезную инфу. */
sealed trait ILkCommonUselessPopupCloseAction extends ILkCommonPopupCloseAction

/** Экшен закрытия попапа с инфой по узлу. */
case object NodeInfoPopupClose extends ILkCommonPopupCloseAction

/** Клик по кнопке закрытия окна-попапа ожидания. */
case object PleaseWaitPopupCloseClick extends ILkCommonUselessPopupCloseAction

/** Экшен закрытия попапа с ошибкой. */
case object ErrorPopupCloseClick extends ILkCommonUselessPopupCloseAction

/** Экшен закрытия вообще всех попапов. */
case object CloseAllPopups extends ILkCommonPopupCloseAction


/** Клик по кнопке ОК в попапе подтверждения удаления чего-то. */
case object DeleteConfirmPopupOk extends ILkCommonAction

/** Клик по кнопке отмены в попапе подтверждения удаления чего-либо. */
case object DeleteConfirmPopupCancel extends ILkCommonPopupCloseAction
