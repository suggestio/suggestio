package io.suggest.jd.render.c

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.render.m.{GridRebuild, MJdRuntime, QdBoundsMeasured}
import io.suggest.jd.tags.JdTag
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import scalaz.Tree

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

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сообщение о завершении измерения высоты внеблокового qd-контента.
    case m: QdBoundsMeasured =>

      // Нужно найти тег в MJdRuntime и решить, что же делать дальше.
      val v0 = value

      val boundsSz2 = MSize2di(
        width  = m.bounds.width.toInt,
        height = m.bounds.height.toInt,
      )

      val qdBl0 = v0.qdBlockLess

      // Т.к. react-measure склонна присылать bounds дважды (согласно докам), то сначала смотрим уже записанные данные.
      qdBl0
        .get( m.jdTag )
        .filterNot( _ contains boundsSz2 )
        .fold {
          // Повторный сигнал размера или размер не изменился. Или сигнал от неизвестного тега.
          noChange

        } { szPot0 =>
          // Т.к. HashMap. то HashMap().updated() не вызывает полной пересборки kv-массива.
          val qdBl2 = qdBl0 + (m.jdTag -> (szPot0 ready boundsSz2))

          val v2 = (MJdRuntime.qdBlockLess set qdBl2)(v0)

          // Нужно понять, остались ли ещё внеблоковые qd-теги, от которых ожидаются размеры.
          // true - Пере-рендер плитки не требуется, т.к. в очереди есть ещё qd-bounds-экшены, помимо этого.
          // false - Больше не надо дожидаться экшенов от других qd-тегов, запускаем новый рендер плитки:
          val hasMoreBlQdsAwaiting = qdBl2
            .valuesIterator
            .exists(_.isEmpty)

          if (hasMoreBlQdsAwaiting)
            updatedSilent(v2)
          else
            updated(v2, GridRebuild.toEffectPure)
        }

  }

}
