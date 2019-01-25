package io.suggest.sc.m.dia

import io.suggest.perm.IPermissionState
import io.suggest.sc.m.ISc3Action
import io.suggest.sc.m.dia.first.MWzPhase

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 17:31
  * Description: Экшены для визарда/визардов.
  */
sealed trait IDiaAction extends ISc3Action


/** Скрыто отрендерить диалог первого запуска, чтобы была анимация на первом шаге. */
case class InitFirstRunWz( isRendered: Boolean ) extends IDiaAction

/** Клик по кнопкам "да" или "нет". */
case class YesNoWz( yesNo: Boolean ) extends IDiaAction


// TODO Надо объеденить оба экшена.
/** Экшен донесения состояния пермишшена. */
case class PermissionState(tryPerm: Try[IPermissionState], phase: MWzPhase) extends IDiaAction

/** Экшен результата реального запроса пермишшена у юзера.
  *
  * @param phase Фаза, в рамках которой был получен ответ.
  * @param res Ответ, если есть. None - таймаут.
  */
case class WzPhasePermRes(phase: MWzPhase, res: Option[IPermissionState]) extends IDiaAction
