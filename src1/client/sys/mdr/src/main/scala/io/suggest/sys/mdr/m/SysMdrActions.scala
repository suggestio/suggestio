package io.suggest.sys.mdr.m

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 14:58
  * Description: Экшены подсистемы модерации sys-mdr.
  */
sealed trait ISysMdrAction extends DAction

/** Экшен реагирования на клик по кнопке аппрува или отказа в этом. */
case class ApproveOrDismiss( info: MMdrActionInfo, isApprove: Boolean ) extends ISysMdrAction

/** Форма подтверждения отказа в размещении. */
case class DismissSubmit( info: MMdrActionInfo, reason: String ) extends ISysMdrAction


/** Запросить с сервера данные узла, который требуется промодерировать. */
case object MdrNextNode extends ISysMdrAction
