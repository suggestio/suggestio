package models.madn.mapf

import io.suggest.model.n2.node.MNode
import models.adv.form.IAdvForAdFormCommonTplArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 16:20
  * Description: Модель аргументов для шаблона [[views.html.lk.adn.mapf.AdnMapTpl]].
  */

trait IAdnMapTplArgs extends IAdvForAdFormCommonTplArgs {

  /** Текущий ADN-узел. */
  def mnode   : MNode

  /** Сериализованные данные состояния react-формы размещения узла. */
  def formB64 : String

}


/** Дефолтовая реализация модели [[IAdnMapTplArgs]]. */
case class MAdnMapTplArgs(
  override val mnode      : MNode,
  override val formB64    : String
)
  extends IAdnMapTplArgs
