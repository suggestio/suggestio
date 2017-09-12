package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.common.geom.coord.MCoords2dD
import io.suggest.jd.tags.{IDocTag, Strip}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.17 18:24
  * Description: Модель состояния таскания jd-тегов.
  */
object MJdDndS {

  def empty = MJdDndS()

  /** Поддержка FastEq для интсансов [[MJdDndS]]. */
  implicit object MJdDndSFastEq extends FastEq[MJdDndS] {
    override def eqv(a: MJdDndS, b: MJdDndS): Boolean = {
      (a.jdt eq b.jdt) &&
        (a.dragOverStrip eq b.dragOverStrip)
    }
  }

}


/** Класс модели состояния Drag-n-Drop, которая влияет на рендер.
  * Часть данных пропихивается через event.dataTransfer, а не здесь.
  *
  * Модель неявно-пустая, т.к. DnD может начинаться за пределами текущей страницы.
  *
  * @param jdt Инстанс docTag'а. Тут два варианта:
  *            - Если DnD внутри страницы, то это инстанс IDocTag из текущего документа.
  *            - ?? Если DnD извне, то тут будет десериализованный инстанс ??
  *
  * @param dragOverStrip Сейчас происходит перетаскивание над указанным strip'ом.
  *                      Используется для подсветки принимающего стрипа.
  * @param pageCoords0 Начальные координаты таскаемого контента в начале перетаскивания.
  * @param coords Координаты перетаскиваемого элемента.
  * @param clXyDiff Разница координат верхнего левого угла и mouse pointer'а.
  */
case class MJdDndS(
                    jdt             : Option[IDocTag]     = None,
                    dragOverStrip   : Option[Strip]       = None,
                    pageCoords0     : Option[MCoords2dD]  = None,
                    coords          : Option[MCoords2dD]  = None,
                    clXyDiff        : Option[MCoords2dD]  = None
                  )
  extends EmptyProduct
{

  def withJdt( jdt: Option[IDocTag] )                     = copy(jdt = jdt)
  def withDragOverStrip( dragOverStrip: Option[Strip] )   = copy( dragOverStrip = dragOverStrip )
  def withCoords(coords: Option[MCoords2dD] )        = copy( coords = coords )

}
