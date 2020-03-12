package io.suggest.sc.c.dev

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{JsScreenUtil, MScreenInfo}
import io.suggest.jd.render.m.GridRebuild
import io.suggest.sc.m.dev.MScScreenS
import io.suggest.sc.m.inx.{MScSideBars, ScCssReBuild, SideBarOpenClose}
import io.suggest.sc.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.c.search.SearchAh
import io.suggest.sjs.dom.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 10:46
  * Description: Контроллер, слушающий события экрана устройства.
  */
class ScreenAh[M](
                   modelRW: ModelRW[M, MScScreenS],
                   rootRO : ModelRO[MScRoot]
                 )
  extends ActionHandler(modelRW)
{

  /** Кол-во миллисекунд срабатывания таймера задержки реакции на произошедший ресайз. */
  private def RSZ_TIMER_MS = 100


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал изменения размеров/ориентации экрана.
    case ScreenReset =>
      val v0 = value
      // TODO Проверять, изменился ли экран.
      v0.rszTimer.fold {
        val tp = DomQuick.timeoutPromise(RSZ_TIMER_MS)
        val fx = Effect {
          for (_ <- tp.fut) yield {
            ScreenRszTimer
          }
        }
        val v2 = MScScreenS.rszTimer.set( Some(tp.timerId) )(v0)
        updatedSilent(v2, fx)

      } { _ =>
        // Таймер уже запущен, просто обновить screen в состоянии свежим инстансом.
        noChange
      }


    // Сигнал срабатывания таймера отложенной реакции на изменение размеров экрана.
    case ScreenRszTimer =>
      // TODO Opt Проверять, изменился ли экран по факту? Может быть изменился и вернулся назад за время таймера?

      // Уведомить контроллер плитки, что пора пересчитать плитку.
      val gridReConfFx = GridRebuild(force = false).toEffectPure
      // Забыть о сработавшем таймере.
      val screen2 = JsScreenUtil.getScreen()
      val uo2 = JsScreenUtil.getScreenUnsafeAreas(screen2)

      val v0 = value

      val v2 = v0.copy(
        info = v0.info.copy(
          screen        = screen2,
          unsafeOffsets = uo2
        ),
        rszTimer = None
      )

      // Аккамулируем эффект. Сначала перестройка основной вёрстки.
      var fx: Effect = ScCssReBuild.toEffectPure + gridReConfFx

      // Если гео.карта видна юзера, то пнуть её после обновления вёрстки.
      val root = rootRO.value
      for {
        lInstance <- root.index.search.geo.data.lmap
        if root.index.search.panel.opened
      } {
        fx >> SearchAh.mapResizeFx( lInstance )
      }

      updated(v2, fx)


    // Отладка: управление коэфф сдвига выдачи.
    case m: UpdateUnsafeScreenOffsetBy =>
      val v0 = value
      val uo0 = v0.info.unsafeOffsets

      val incDecF: Option[Int] => Option[Int] = if (uo0.isEmpty) {
        // Нет исходного сдвига. Скорее всего, сдвиги на этом устройстве не актуальны, но увеличиваем сразу все поля.
        offOpt0: Option[Int] =>
          val off2 = offOpt0.getOrElse(0) + m.incDecBy
          OptionUtil.maybe( off2 > 0 )(off2)
      } else {
        // Обновить только ненулевые значения. Это основной вектор отладки.
        offOpt0: Option[Int] =>
          for {
            off0 <- offOpt0
            off1 = off0 + m.incDecBy
            if off1 > 0
          } yield off1
      }

      val uo2 = uo0.copy(
        topO    = incDecF(uo0.topO),
        leftO   = incDecF(uo0.leftO),
        rightO  = incDecF(uo0.rightO),
        bottomO = incDecF(uo0.bottomO)
      )

      val v2 = MScScreenS.info
        .composeLens( MScreenInfo.unsafeOffsets )
        .set(uo2)(v0)

      // По идее, ребилдить можно прямо тут, но zoom-модель не позволяет отсюда получить доступ к scCss.
      // Выполнить ребилд ScCss в фоне:
      var fx = ScCssReBuild.toEffectPure

      // Если закрыта левая панель меню, то нужно её раскрыть (нужна как ориентир, иначе непонятно).
      if (!rootRO.value.index.menu.opened)
        fx = SideBarOpenClose( MScSideBars.Menu, open = true ).toEffectPure >> fx

      updated(v2, fx)

  }

}
