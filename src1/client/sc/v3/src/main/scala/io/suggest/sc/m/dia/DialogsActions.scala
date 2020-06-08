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


/** Скрыто отрендерить диалог первого запуска, чтобы была анимация на первом шаге.
  *
  * @param showHide true - Инициализация с возможным отображением диалога первого запуска,
  *                 если это действительно первый запуск.
  *                 false - Деинициализация (сокрытие).
  */
case class InitFirstRunWz( showHide: Boolean ) extends IDiaAction

/** Клик по кнопкам "да" или "нет". */
case class YesNoWz( yesNo: Boolean ) extends IDiaAction


/** Выставлено значение для subscribe-функции. */
case class Wz1SetUnSubscribeF(unSubscribeF: () => Unit ) extends IDiaAction

case object Wz1RebuildCss extends IDiaAction

/** Экшен результата реального запроса пермишшена у юзера.
  *
  * @param phase Фаза, в рамках которой был получен ответ.
  * @param res Ответ, если есть. None - таймаут.
  */
case class WzPhasePermRes(phase: MWzPhase, res: Try[IPermissionState]) extends IDiaAction
