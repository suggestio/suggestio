package util.blocks

import enumeratum.values.{IntEnum, IntEnumEntry}
import models.blk
import models.mctx.Context
import play.twirl.api.{Html, Template2}
import views.html.blocks._

// TODO Надо выпилить эту модель, т.к. динамический редактор с кучей карточек и переключаемыми блоками ушел в небытие.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Устаревшая модель конфигов для блоков верстки.
 * Надо удалить его полностью.
 */

object BlocksConf extends IntEnum[BlockConf] {

  // Начало значений

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  case object Block20 extends BlockConf( 20 ) {
    override def template = _block20Tpl
  }

  override def values = findValues

}



sealed abstract class BlockConf( override val value: Int ) extends IntEnumEntry {

  /** Флаг того, что на блок не стоит навешивать скрипты, отрабатывающие клик по нему. */
  def hrefBlock = false

  /** Шаблон для рендера. */
  def template: Template2[blk.IRenderArgs, Context, Html]

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(args: blk.IRenderArgs)(implicit ctx: Context) = {
    template.render(args, ctx)
  }

}
