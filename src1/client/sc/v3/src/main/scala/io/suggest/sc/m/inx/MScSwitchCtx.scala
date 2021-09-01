package io.suggest.sc.m.inx

import diode.Effect
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoLoc
import io.suggest.sc.index.MScIndexArgs
import io.suggest.text.StringUtil
import japgolly.univeq._
import monocle.macros.GenLens

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


  type ViewsAction = Option[Boolean]
  object ViewsAction {
    /** Замена index.state.views на новый индекс. */
    final def RESET: ViewsAction = None
    /** Добавление нового индекса с сохранением старого. */
    final def PUSH: ViewsAction = OptionUtil.SomeBool.someTrue
    /** Убрать последний индекс из стопки. */
    final def POP: ViewsAction = OptionUtil.SomeBool.someFalse
  }

  final def INDEX_MAP_RESET_DFLT = true

  def indexMapReset = GenLens[MScSwitchCtx](_.indexMapReset)

}


/** Инстанс модель живёт прямо в экшенах, но в некоторых случаях - в состоянии.
  *
  * @param focusedAdId Фокусироваться на id карточки.
  * @param demandLocTest Это процесс проверки смены локации после ре-активации существующей выдачи?
  * @param indexQsArgs Аргументы для запроса индекса.
  * @param forceGeoLoc Форсировать указанную геолокацию для запроса индекса.
  * @param showWelcome Поправка на отображение приветствия.
  * @param afterIndex После переключения - что сделать?
  * @param afterBack Эффект при переходе назад. Требует storePrevIndex=true или иных условий для IndexAh._indexUpdated().
  * @param afterBackGrid Эффект после "назад" и после получения и обработки начальной порции блоков плитки.
  * @param indexMapReset Обновить гео.карту по индексу узла с сервера.
  */
case class MScSwitchCtx(
                         indexQsArgs      : MScIndexArgs,
                         focusedAdId      : Option[String]    = None,
                         demandLocTest    : Boolean           = false,
                         forceGeoLoc      : Option[MGeoLoc]   = None,
                         showWelcome      : Boolean           = true,
                         afterIndex       : Option[Effect]    = None,
                         afterBack        : Option[Effect]    = None,
                         afterBackGrid    : Option[Effect]    = None,
                         viewsAction      : MScSwitchCtx.ViewsAction   = MScSwitchCtx.ViewsAction.RESET,
                         indexMapReset    : Boolean           = MScSwitchCtx.INDEX_MAP_RESET_DFLT,
                       ) {

  override def toString: String = StringUtil.toStringHelper(this) { f =>
    if (indexQsArgs !=* MScIndexArgs.default) f("indexQS")(indexQsArgs)
    focusedAdId foreach f("foc")
    if (demandLocTest) f("demand")(demandLocTest)
    forceGeoLoc foreach f("forceGeoLoc")
    if (!showWelcome) f("showWc")(showWelcome)
    afterIndex foreach f("afterInx")
    afterBack foreach f("afterBack")
    afterBackGrid foreach f("afterBackGrid")
    if (viewsAction !=* MScSwitchCtx.ViewsAction.RESET) f("viewsAction")(viewsAction)
    if (indexMapReset !=* MScSwitchCtx.INDEX_MAP_RESET_DFLT) f("inxMapRst")(indexMapReset)
  }

}

