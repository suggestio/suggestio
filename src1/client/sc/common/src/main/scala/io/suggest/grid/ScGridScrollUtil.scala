package io.suggest.grid

import diode.Effect
import io.suggest.ad.blk.BlockPaddings
import io.suggest.grid.build.MGridBuildResult
import io.suggest.sc.model.grid.{MGridS, MScAdData}
import io.suggest.sc.view.styl.ScCss
import io.suggest.scroll.IScrollApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DoNothing
import io.suggest.text.StringUtil
import org.scalajs.dom


object ScGridScrollUtil {

  /** Рандомный id для grid-wrapper. */
  // TODO Если используем рандомные id, то надо избежать комбинаций букв, приводящих к срабатываниям блокировщиков рекламы.
  lazy val SCROLL_CONTAINER_ID = StringUtil.randomIdLatLc()

}


class ScGridScrollUtil(
                        scrollApiOpt: Option[IScrollApi],
                      ) {

  /** Нечистый метод чтения текущего скролла через ковыряния внутри view'а,
    * чтобы не усложнять модели и всю логику обработки скролла.
    * Следует дёргать внутри Effect().
    */
  def getGridScrollTop(): Option[Double] = {
    Option( dom.document.getElementById( ScGridScrollUtil.SCROLL_CONTAINER_ID ) )
      .map( _.scrollTop )
  }


  /** Эффект скроллинга к указанной карточке. */
  // TODO Унести на уровень view (в GridR), законнектится на поле MGridS().interactWith для инфы по скроллу.
  def scrollToAdFx(toAd    : MScAdData,
                   gbRes   : MGridBuildResult
                  ): Option[Effect] = {
    // Карточка уже открыта, её надо свернуть назад в main-блок.
    // Нужно узнать координату в плитке карточке
    for (scrollApi <- scrollApiOpt) yield {
      Effect.action {
        toAd
          .gridItems
          .iterator
          .flatMap { gridItem =>
            gbRes.coordsById.get( gridItem.jdDoc.tagId )
          }
          // Взять только самый верхний блок карточки. Он должен быть первым по порядку:
          .nextOption()
          .foreach { toXY =>
            scrollApi.scrollTo(
              ScGridScrollUtil.SCROLL_CONTAINER_ID,
              relative = false,
              // Сдвиг обязателен, т.к. карточки заезжают под заголовок.
              toPx = Math.max(0, toXY.y - ScCss.HEADER_HEIGHT_PX - BlockPaddings.default.value),
              smooth = true,
            )
          }

        DoNothing
      }
    }
  }


  /** Восстановление скролла после добавления
    *
    * @param g0 Начальное состояние плитки.
    * @param g2 Новое состояние плитки.
    * @return
    */
  def repairScrollPosFx(g0: MGridS, g2: MGridS): Option[Effect] = {
    // Нужно скроллить НЕанимированно, т.к. неявная коррекция выдачи должна проходить мгновенно и максимально незаметно.
    // Для этого надо вычислить разницу высоты между старой плиткой и новой плиткой, и скорректировать текущий скролл
    // на эту разницу без какой-либо анимации TODO (за искл. около-нулевого исходного скролла).
    for {
      scrollApi <- scrollApiOpt
      gridHeightPx0 = g0.core.gridBuild.gridWh.height
      gridHeightPx2 = g2.core.gridBuild.gridWh.height
      gridHeightDeltaPx = gridHeightPx2 - gridHeightPx0
      if Math.abs(gridHeightDeltaPx) > 2
    } yield {
      // Есть какой-то заметный глазу скачок высоты плитки. Запустить эффект сдвига скролла плитки.
      Effect.action {
        // Нужно понять, есть ли скролл прямо сейчас: чтобы не нагружать состояние лишним мусором, дёргаем элемент напрямую.
        if ( getGridScrollTop().exists(_ > 1) ) {
          scrollApi.scrollTo(
            ScGridScrollUtil.SCROLL_CONTAINER_ID,
            relative = true,
            toPx = gridHeightDeltaPx,
            smooth = false,
          )
        }

        DoNothing
      }
    }
  }

}
