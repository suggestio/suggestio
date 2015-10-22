package models.mlk

import models.im.{MImg, MImgT}
import models.im.logo.LogoOpt_t
import models.{MMeta, MNode, MAdT}
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 12:04
 * Description: Модели аргументов для редактирования узла (магазина) в личном кабинете.
 */

/** Модель, отражающая результирующее значение биндинга формы редактирования узла. */
case class FormMapResult(
  meta        : MMeta,
  logoOpt     : LogoOpt_t,
  waImgOpt    : Option[MImgT]   = None,
  gallery     : List[MImg]      = Nil
)


/** Интерфейс контейнера аргументов вызова nodeEditTpl. */
trait INodeEditArgs {

  /** Редактируемый узел. */
  def mnode         : MNode

  /** Маппинг формы редактирования узла. */
  def mf            : Form[FormMapResult]

  /** Данные по текущей карточке приветствия. */
  def welcomeAdOpt  : Option[MAdT]

}


/** Дефолтовая реализация контейнера передаваемых аргументов для nodeEditTpl. */
case class NodeEditArgs(
  override val mnode         : MNode,
  override val mf            : Form[FormMapResult],
  override val welcomeAdOpt  : Option[MAdT]
)
  extends INodeEditArgs
