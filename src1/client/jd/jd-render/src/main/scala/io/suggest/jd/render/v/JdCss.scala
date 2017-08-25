package io.suggest.jd.render.v

import scalacss.DevDefaults._
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{AbsPos, IDocTag, Strip}

import scala.reflect.ClassTag
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:40
  * Description: Динамические CSS-стили для рендера блоков плитки.
  *
  * Таблица стилей плоская для всех документов сразу.
  */

class JdCss( jdCssArgs: MJdCssArgs )
  extends StyleSheet.Inline {

  import dsl._

  // TODO Вынести статические стили в ScCss?
  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    addClassName("sm-block")
  )

  /** Ширина и длина -- 100%. */
  val wh100 = {
    val pc100 = 100.%%
    style(
      width(pc100),
      height(pc100)
    )
  }


  /** Итератор тегов указанного типа (класса) со всех уровней. */
  private def _jdTagsIter[T <: IDocTag : ClassTag]: Iterator[T] = {
    jdCssArgs.templates
      .iterator
      .flatMap(_.deepChildrenIter)
      .flatMap {
        case ap: T => ap :: Nil
        case _ => Nil
      }
  }

  /** Сборка домена для всех указанных тегов из всех документов. */
  private def _mkJdTagDomain[T <: IDocTag : ClassTag]: Domain[T] = {
    val absPoses = _jdTagsIter[T]
      .toIndexedSeq
    new Domain.OverSeq( absPoses )
  }



  // -------------------------------------------------------------------------------
  // Strip
  private val _stripsDomain = _mkJdTagDomain[Strip]

  /** Стили контейнеров полосок. */
  val stripOuterStyleF = styleF(_stripsDomain) { strip =>
    // Стиль фона полосы
    val stylBg = strip.bgColor.fold(StyleS.empty) { mcd =>
      styleS(
        backgroundColor(Color(mcd.hexCode))
      )
    }

    // Стиль размеров блока-полосы.
    val stylWh = strip.bm.fold(StyleS.empty) { bm =>
      styleS(
        width( bm.width.px ),
        height( bm.height.px )
      )
    }

    // Объединить все стили одного стрипа.
    stylWh.compose( stylBg )
  }



  // -------------------------------------------------------------------------------
  // AbsPos

  private val _absPosDomain = _mkJdTagDomain[AbsPos]

  /** Общий стиль для всех AbsPos-тегов. */
  val absPosStyleAll = style(
    position.absolute,
    zIndex(5)
  )

  /** Стили для элементов, отпозиционированных абсолютно. */
  val absPosStyleF = styleF(_absPosDomain) { absPos =>
    styleS(
      top( absPos.topLeft.y.px ),
      left( absPos.topLeft.x.px )
    )
  }

}


import com.softwaremill.macwire._

/** DI-factory для сборки инстансов [[JdCss]]. */
class JdCssFactory {

  def mkJdCss( jdCssArgs: MJdCssArgs ): JdCss = {
    wire[JdCss]
  }

}
