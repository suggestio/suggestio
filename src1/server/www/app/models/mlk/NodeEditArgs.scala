package models.mlk

import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.node.meta.MMeta
import io.suggest.url.MHostInfo
import models.im.MImgT
import models.im.logo.LogoOpt_t
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 12:04
 * Description: Модели аргументов для редактирования узла (магазина) в личном кабинете.
 * @see [[views.html.lk.adn.edit.nodeEditTpl]]
 * @see [[views.html.lk.adn.edit._leaderNodeFormTpl]]
 */

case class NodeEditArgs(
                         mnode         : MNode,
                         mf            : Form[FormMapResult],
                         mediaHostsMap : Map[String, Seq[MHostInfo]]
                       )


/** Модель, отражающая результирующее значение биндинга формы редактирования узла. */
case class FormMapResult(
  meta        : MMeta,
  logoOpt     : LogoOpt_t,
  waImgOpt    : Option[MImgT]   = None,
  gallery     : List[MImgT]     = Nil
)


