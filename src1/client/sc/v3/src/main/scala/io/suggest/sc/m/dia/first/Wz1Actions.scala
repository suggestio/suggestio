package io.suggest.sc.m.dia.first

import diode.Effect
import io.suggest.perm.IPermissionState
import io.suggest.sc.m.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 17:31
  * Description: Экшены для визарда/визардов.
  */
sealed trait IWz1Action extends ISc3Action


/** Скрыто отрендерить диалог первого запуска, чтобы была анимация на первом шаге.
  *
  * @param showHide true - Инициализация с возможным отображением диалога первого запуска,
  *                 если это действительно первый запуск.
  *                 false - Деинициализация (сокрытие).
  */
case class InitFirstRunWz( showHide: Boolean ) extends IWz1Action

/** Клик по кнопкам "да" или "нет". */
case class YesNoWz( yesNo: Boolean ) extends IWz1Action


/** Экшен результата реального запроса пермишшена у юзера.
  *
  * @param phase Фаза, в рамках которой был получен ответ.
  * @param res Ответ, если есть. None - таймаут.
  */
case class WzPhasePermRes(phase: MWzPhase,
                          res: Try[IPermissionState],
                          reason: Option[WzReadPermissions] = None,
                         )
  extends IWz1Action


/** Async reading all current permissions states into wz's state perms.map. */
case class WzReadPermissions(onComplete: Option[Effect] = None ) extends IWz1Action