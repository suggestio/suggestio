package util.blocks

import io.suggest.common.menum.EnumValue2Val
import io.suggest.util.logs.MacroLogsImpl
import models.blk
import models.mctx.Context
import play.twirl.api.{Html, Template2}
import util.FormUtil.IdEnumFormMappings
import views.html.blocks._

// TODO Надо выпилить эту модель, т.к. динамический редактор с кучей карточек и переключаемыми блоками ушел в небытие.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Устаревшая модель конфигов для блоков верстки.
 * Надо удалить его полностью.
 *
 */

object BlocksConf
  extends Enumeration
  with EnumValue2Val
{

  /** Всё описание блока идёт через наследование Val и её интерфейса [[ValT]] при необходимости. */
  protected abstract class Val(id: Int)
    extends super.Val(id)
    with CommonBlock2T

  override type T = Val


  // Начало значений

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait CommonBlock2T extends ValT

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait Block20t extends CommonBlock2T {
    override def template = _block20Tpl
  }
  val Block20 = new Val( 20 ) with Block20t {
  }


}



/** Базовый интерфейс для реализаций класса Enumeration.Val. */
trait ValT {

  def id: Int

  /** Флаг того, что на блок не стоит навешивать скрипты, отрабатывающие клик по нему. */
  def hrefBlock = false

  /** Шаблон для рендера. */
  def template: Template2[blk.IRenderArgs, Context, Html]

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(args: blk.IRenderArgs)(implicit ctx: Context) = {
    template.render(args, ctx)
  }

}
