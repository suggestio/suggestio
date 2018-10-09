package io.suggest.sys.mdr.m

import io.suggest.spa.DAction
import io.suggest.sys.mdr.{MMdrActionInfo, MNodeMdrInfo}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 14:58
  * Description: Экшены подсистемы модерации sys-mdr.
  */
sealed trait ISysMdrAction extends DAction

/** Экшен реагирования на клик по кнопке аппрува или отказа в этом. */
case class ApproveOrDismiss( info: MMdrActionInfo, isApprove: Boolean ) extends ISysMdrAction

/** Ответ сервера по команде модерации. */
case class DoMdrResp(timestampMs: Long, info: MMdrActionInfo, tryResp: Try[_]) extends ISysMdrAction

/** Запросить с сервера данные узла, который требуется промодерировать. */
case object MdrNextNode extends ISysMdrAction

/** Ответ на запрос next-node для модерации. */
case class MdrNextNodeResp(
                            timestampMs   : Long,
                            tryResp       : Try[Option[MNodeMdrInfo]]
                          )
  extends ISysMdrAction


/** Юзер вводит причину отказа в поле причины. */
case class SetDismissReason(reason: String) extends ISysMdrAction

/** Клик по кнопке подтверждения отказа в размещении. */
case object DismissOkClick extends ISysMdrAction

/** Клик по кнопке отмены диалога отказа. */
case object DismissCancelClick extends ISysMdrAction
