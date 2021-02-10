package io.suggest.sc.m.inx

import diode.Effect
import io.suggest.geo.MGeoLoc
import io.suggest.sc.index.MScIndexArgs
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 14:57
  * Description: Обновление всей выдачи (с узла на узел, например) - процесс в несколько этапов:
  * [геолокация], запрос индекса выдачи, [запрос подтверждения], [плитка].
  */

object MScSwitchCtx {

  @inline implicit def univEq: UnivEq[MScSwitchCtx] = UnivEq.force


  implicit final class SwitchOptExt(private val opt: Option[MScSwitchCtx]) extends AnyVal {

    def showWelcome: Boolean =
      opt.fold(true)(_.showWelcome)

  }

}


/** Инстанс модель живёт прямо в экшенах, но в некоторых случаях - в состоянии.
  *
  * @param focusedAdId Фокусироваться на id карточки.
  * @param demandLocTest Это процесс проверки смены локации после ре-активации существующей выдачи?
  * @param indexQsArgs Аргументы для запроса индекса.
  * @param forceGeoLoc Форсировать указанную геолокацию для запроса индекса.
  * @param showWelcome Поправка на отображение приветствия.
  * @param storePrevIndex Сохранить в state.views состояние предыдущего индекса.
  * @param afterSwitch После переключения - что сделать?
  * @param afterBack Эффект при переходе назад. Требует storePrevIndex=true или иных условий для IndexAh._indexUpdated().
  */
case class MScSwitchCtx(
                         indexQsArgs      : MScIndexArgs,
                         focusedAdId      : Option[String]    = None,
                         demandLocTest    : Boolean           = false,
                         forceGeoLoc      : Option[MGeoLoc]   = None,
                         showWelcome      : Boolean           = true,
                         storePrevIndex   : Boolean           = false,
                         afterSwitch      : Option[Effect]    = None,
                         afterBack        : Option[Effect]    = None,
                       )

