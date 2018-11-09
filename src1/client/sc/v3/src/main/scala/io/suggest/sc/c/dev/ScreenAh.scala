package io.suggest.sc.c.dev

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.m.dev.MScScreenS
import io.suggest.sc.m.grid.GridReConf
import io.suggest.sc.m.inx.ScCssReBuild
import io.suggest.sc.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sc.c.search.SearchAh

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

  private def scCssRebuildFx: Effect =
    ScCssReBuild.toEffectPure


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
        val v2 = v0.withRszTimer( Some( tp.timerId ) )
        updatedSilent(v2, fx)

      } { _ =>
        // Таймер уже запущен, просто обновить screen в состоянии свежим инстансом.
        noChange
      }


    // Сигнал срабатывания таймера отложенной реакции на изменение размеров экрана.
    case ScreenRszTimer =>
      // TODO Opt Проверять, изменился ли экран по факту? Может быть изменился и вернулся назад за время таймера?

      // Уведомить контроллер плитки, что пора пересчитать плитку.
      val gridReConfFx = GridReConf.toEffectPure
      // Забыть о сработавшем таймере.
      val screen2 = JsScreenUtil.getScreen()
      val uo2 = JsScreenUtil.getScreenUnsafeAreas(screen2)

      val v0 = value

      val v2 = value.copy(
        info = v0.info.copy(
          screen        = screen2,
          unsafeOffsets = uo2
        ),
        rszTimer = None
      )

      // Аккамулируем эффект. Сначала перестройка основной вёрстки.
      var fx: Effect = scCssRebuildFx + gridReConfFx

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
          offOpt0
            .map(_ + m.incDecBy)
            .filter(_ > 0)
      }

      val uo2 = uo0.copy(
        topO    = incDecF(uo0.topO),
        leftO   = incDecF(uo0.leftO),
        rightO  = incDecF(uo0.rightO),
        bottomO = incDecF(uo0.bottomO)
      )

      val v2 = v0.withInfo(
        v0.info.withSafeArea(
          uo2
        )
      )

      // По идее, ребилдить можно прямо тут, но zoom-модель не позволяет отсюда получить доступ к scCss.
      // Выполнить ребилд ScCss в фоне:
      val fx = scCssRebuildFx
      updated(v2, fx)

  }

}
