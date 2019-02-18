package models.msc

import io.suggest.model.n2.node.MNode
import models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:25
 * Description: Контейнер аргументов, необходимых для рендера карточек выдачи.
 */
case class AdBodyTplArgs(
                          brArgs        : blk.IRenderArgs,
                          producer      : MNode,
                        )

