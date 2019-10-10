package io.suggest.jd.render.c

import com.github.souporserious.react.measure.WhJs
import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.render.m.{GridRebuild, MJdCssArgs, MJdRuntime, MJdRuntimeData, MQdBlSize, QdBoundsMeasured}
import io.suggest.jd.render.v.JdCss
import io.suggest.jd.tags.JdTag
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import scalaz.Tree

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.2019 11:19
  * Description: Контроллер для каких-то внутренних callback'ов при любом jd-рендере.
  */
class JdAh[M](
               templatesRO    : ModelRO[Stream[Tree[JdTag]]],
               modelRW        : ModelRW[M, MJdRuntime],
             )
  extends ActionHandler(modelRW)
{

  private def _boundsUndef2sz2d(szUnd: js.UndefOr[WhJs]): MSize2di = {
    val bounds = szUnd.get
    MSize2di(
      width  = bounds.width.toInt,
      height = bounds.height.toInt,
    )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сообщение о завершении измерения высоты внеблокового qd-контента.
    case m: QdBoundsMeasured =>

      // Нужно найти тег в MJdRuntime и решить, что же делать дальше.
      val v0 = value

      val qdBlSz = MQdBlSize(
        bounds = _boundsUndef2sz2d( m.contentRect.bounds ),
        client = _boundsUndef2sz2d( m.contentRect.client ),
      )

      val qdBl0 = v0.data.qdBlockLess

      // Т.к. react-measure склонна присылать bounds дважды (согласно докам), то сначала смотрим уже записанные данные.
      qdBl0
        .get( m.jdTag )
        .filterNot( _ contains[MQdBlSize] qdBlSz )
        .fold {
          // Повторный сигнал размера или размер не изменился. Или сигнал от неизвестного тега.
          noChange

        } { szPot0 =>
          // Т.к. HashMap. то HashMap().updated() не вызывает полной пересборки kv-массива.
          val qdBl2 = qdBl0 + (m.jdTag -> (szPot0 ready qdBlSz))

          // Нужно понять, остались ли ещё внеблоковые qd-теги, от которых ожидаются размеры.
          // true - Пере-рендер плитки не требуется, т.к. в очереди есть ещё qd-bounds-экшены, помимо этого.
          // false - Больше не надо дожидаться экшенов от других qd-тегов, запускаем новый рендер плитки:
          val hasMoreBlQdsAwaiting = qdBl2
            .valuesIterator
            .exists(_.isEmpty)

          if (hasMoreBlQdsAwaiting) {
            // Есть ещё ожидаемые данные. Просто тихо обновить состояние:
            val v2 = MJdRuntime.data
              .composeLens(MJdRuntimeData.qdBlockLess)
              .set(qdBl2)(v0)
            updatedSilent(v2)
          } else {
            // Требуется пересборка данных для шаблонов
            val data2 = MJdRuntimeData.qdBlockLess
              .set(qdBl2)(v0.data)
            val v2 = MJdRuntime(
              jdCss = JdCss.jdCssArgs
                .composeLens(MJdCssArgs.data)
                .set(data2)(v0.jdCss),
              data = data2,
            )
            updated(v2, GridRebuild.toEffectPure)
          }
        }

  }

}
