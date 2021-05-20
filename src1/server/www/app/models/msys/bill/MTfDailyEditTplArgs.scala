package models.msys.bill

import io.suggest.n2.bill.tariff.daily.MTfDaily
import io.suggest.n2.node.MNode
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 18:32
 * Description: Аргументы для шаблона страницы редактирования посуточного тарифа.
 */
final case class MTfDailyEditTplArgs(
                                      mnode        : MNode,
                                      mcals        : Seq[MNode],
                                      tf           : Form[MTfDaily]
                                    )
